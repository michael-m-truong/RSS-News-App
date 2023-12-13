package com.bignerdranch.android.newsapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.newsapp.database.NewsFeed
import com.bignerdranch.android.newsapp.databinding.FragmentNewsfeedListBinding
import com.bignerdranch.android.newsapp.models.ReadTimeOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.HashMap

private const val TAG = "NewsFeedListFragment"

class NewsFeedListFragment : Fragment() {

    private val newsFeedListViewModel: NewsFeedListViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private var job: Job? = null
    private var _binding: FragmentNewsfeedListBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private var isMoving = false
    private var fromPos: Int? = null
    private var toPos: Int? = null


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        init_styles()

        _binding = FragmentNewsfeedListBinding.inflate(inflater, container, false)

        binding.newsfeedRecyclerView.layoutManager = LinearLayoutManager(context)

        // Set your touch listener to detect when the touch is released
//        binding.root.setOnTouchListener { _, motionEvent ->
//            if (motionEvent.action == MotionEvent.ACTION_UP) {
//                if (fromPos != null && toPos != null) {
//                    newsFeedListViewModel.reorderNewsFeeds(fromPos!!, toPos!!)
//                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//                        newsFeedListViewModel.updateNewsFeedOrder(fromPos!!, toPos!!)
//                        fromPos = null
//                        toPos = null
//                    }
//                    isMoving =false
//                    //fromPos = null
//                    //toPos = null
//                }
//            }
//            false
//        }

        swipeToDelete()
        createArticleItemTouchHelperCallback()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                newsFeedListViewModel.isListEmpty.collect { isEmpty ->
                    if (isEmpty) {
                        binding.newsfeedRecyclerView.visibility = View.GONE
                        binding.emptyTextView.visibility = View.VISIBLE
                    } else {
                        binding.newsfeedRecyclerView.visibility = View.VISIBLE
                        binding.emptyTextView.visibility = View.GONE
                    }
                }
            }
        }

        return binding.root
    }

    fun init_styles() {
        val actionBar = (activity as AppCompatActivity?)!!.supportActionBar
        actionBar?.title = "News Radar"

        // Set ActionBar text color based on night mode
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val textColor = if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            Color.WHITE // Night mode color
        } else {
            Color.BLACK // Day mode color
        }

        if (actionBar != null) {
            val text: Spannable = SpannableString(actionBar.title)
            text.setSpan(
                ForegroundColorSpan(textColor),
                0,
                text.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            actionBar.title = text
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                newsFeedListViewModel.newsFeeds.collect { newsfeeds ->
                    binding.newsfeedRecyclerView.adapter =
                        NewsFeedListAdapter(newsfeeds) { newsfeedId ->
                            findNavController().navigate(
                                NewsFeedListFragmentDirections.showCrimeDetail(newsfeedId)
                            )
                        }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_newsfeed_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_newsfeed -> {
                showNewNewsFeed()
                true
            }

            R.id.saved_newsfeed -> {
                showSavedNewsFeed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSavedNewsFeed() {

        viewLifecycleOwner.lifecycleScope.launch {
            findNavController().navigate(
                NewsFeedListFragmentDirections.showSavedArticles()
            )
        }
    }

    private fun showNewNewsFeed() {
        viewLifecycleOwner.lifecycleScope.launch {
            val newNewsFeed = NewsFeed(
                id = UUID.randomUUID(),
                title = "",
                date = Date(),
                wordBank = mutableListOf<String>(),
                excludeWordBank = mutableListOf<String>(),
                orderNumber = newsFeedListViewModel.getCount() + 1,
                sortByOption = 0,
                readTimeOption = mutableListOf<ReadTimeOption>(
                    ReadTimeOption.oneTOthree,
                    ReadTimeOption.fourTOsix,
                    ReadTimeOption.sixPlus
                ),
                dateRelevanceOption = 0,
                publisherOption = mutableListOf<String>("INIT_NEWSFEED"),
                sourceOption = HashMap<String, Boolean>().apply {
                    put("Google", true)
                    put("Reddit", false)
                    put("Twitter", false)
                }
            )
            newsFeedListViewModel.addNewsFeed(newNewsFeed)
            findNavController().navigate(
                NewsFeedListFragmentDirections.showCrimeDetail(newNewsFeed.id)
            )
        }
    }

    private fun swipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // Not used in this case
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val newsFeed = newsFeedListViewModel.getNewsFeedByPosition(position)

                val alertdialog = AlertDialog.Builder(context)
                    .setTitle("Delete Item")
                    .setMessage("Are you sure you want to delete this item?")
                    .setPositiveButton("Delete") { _, _ ->
                        if (newsFeed != null) {
                            viewLifecycleOwner.lifecycleScope.launch {
                                newsFeedListViewModel.deleteNewsFeed(newsFeed.id)
                            }
                        }
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss() // Close the dialog when the user clicks "Cancel"
                        viewHolder.bindingAdapter?.notifyItemChanged(position)
                    }
                    .setCancelable(true) // Allow dialog dismissal by clicking outside
                    .setOnCancelListener {
                        // Handle cancellation as needed (equivalent to clicking "Cancel" button)
                        viewHolder.bindingAdapter?.notifyItemChanged(position)
                    }
                    .show()
            }

            // Customize the swipe appearance
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val background = ColorDrawable(Color.RED)
                    val deleteIcon =
                        ContextCompat.getDrawable(requireContext(), R.drawable.baseline_delete_24)
                    val iconMargin = (itemView.height - deleteIcon?.intrinsicHeight!!) / 2

                    // Draw the red background
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)

                    // Draw the delete icon if the swipe is in progress
                    if (isCurrentlyActive) {
                        deleteIcon.setBounds(
                            itemView.right - iconMargin - deleteIcon.intrinsicWidth,
                            itemView.top + iconMargin,
                            itemView.right - iconMargin,
                            itemView.bottom - iconMargin
                        )
                        deleteIcon.draw(c)
                    }
                }

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.newsfeedRecyclerView)
    }

    private fun createArticleItemTouchHelperCallback() {
        val simpleCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END, // Drag directions
            0 // Swipe directions (no swiping)
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition

                recyclerView.adapter?.notifyItemMoved(fromPosition, toPosition)
                if (isMoving) {
                    toPos = toPosition
                } else {
                    fromPos = fromPosition
                    toPos = toPosition
                }
                isMoving = true


                //newsFeedListViewModel.reorderNewsFeeds(fromPosition, toPosition)
                return false
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)

                // Call notifyItemMoved here when the move is complete (user releases the item)
                if (fromPos != null && toPos != null) {
                    newsFeedListViewModel.reorderNewsFeeds(fromPos!!, toPos!!)
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        newsFeedListViewModel.updateNewsFeedOrder(fromPos!!, toPos!!)
                        fromPos = null
                        toPos = null
                    }
                    isMoving = false
                    //fromPos = null
                    //toPos = null
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No action needed for swiping, so this method is left empty
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(binding.newsfeedRecyclerView)


    }


}