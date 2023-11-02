package com.bignerdranch.android.newsapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.graphics.Canvas
import android.util.Log
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bignerdranch.android.newsapp.databinding.FragmentNewsfeedListBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.Date
import java.util.UUID
import android.app.AlertDialog

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
    ): View? {
        _binding = FragmentNewsfeedListBinding.inflate(inflater, container, false)

        binding.newsfeedRecyclerView.layoutManager = LinearLayoutManager(context)

        // Set your touch listener to detect when the touch is released
//        binding.root.setOnTouchListener { _, motionEvent ->
//            if (motionEvent.action == MotionEvent.ACTION_UP) {
//                Log.d("from", fromPos.toString())
//                Log.d("to", toPos.toString())
//                if (fromPos != null && toPos != null) {
//                    Log.d("changed", toPos.toString())
//                    newsFeedListViewModel.reorderNewsFeeds(fromPos!!, toPos!!)
//                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//                        Log.d("changed", "here")
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
                        binding.actionButton.visibility = View.VISIBLE
                    } else {
                        binding.newsfeedRecyclerView.visibility = View.VISIBLE
                        binding.emptyTextView.visibility = View.GONE
                        binding.actionButton.visibility = View.GONE
                    }
                }
            }
        }

        binding.actionButton.setOnClickListener {
            showNewNewsFeed()
        }

        return binding.root
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

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showNewNewsFeed() {
        viewLifecycleOwner.lifecycleScope.launch {
            val newNewsFeed = NewsFeed(
                id = UUID.randomUUID(),
                title = "",
                date = Date(),
                wordBank = mutableListOf<String>(),
                orderNumber = newsFeedListViewModel.getCount() + 1
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

                AlertDialog.Builder(context)
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
                        deleteIcon?.setBounds(
                            itemView.right - iconMargin - deleteIcon.intrinsicWidth,
                            itemView.top + iconMargin,
                            itemView.right - iconMargin,
                            itemView.bottom - iconMargin
                        )
                        deleteIcon?.draw(c)
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
                //Log.d("moving","moving")
                if (isMoving) {
                    toPos = toPosition
                }
                else {
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
                Log.d("noway","noway")
                if (fromPos != null && toPos != null) {
                    Log.d("changed", toPos.toString())
                    newsFeedListViewModel.reorderNewsFeeds(fromPos!!, toPos!!)
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        Log.d("changed", "here")
                        newsFeedListViewModel.updateNewsFeedOrder(fromPos!!, toPos!!)
                        fromPos = null
                        toPos = null
                    }
                    isMoving =false
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