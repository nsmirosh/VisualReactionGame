package apps.smoll.dragdropgame.features.game

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.DragEvent
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import apps.smoll.dragdropgame.R
import apps.smoll.dragdropgame.Shape
import apps.smoll.dragdropgame.utils.*
import kotlinx.android.synthetic.main.fragment_main.*
import timber.log.Timber


class MainFragment : Fragment(R.layout.fragment_main) {

    val gameViewModel: GameViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gameViewModel.startGame(screenWidthAndHeight)
        startObservingLiveData()
        initListeners()
    }

    private fun startObservingLiveData() {
        gameViewModel.scoreLiveData.observe(
            viewLifecycleOwner,
            { updateScoreText(it) }
        )
        gameViewModel.shapeToMatchLiveData.observe(
            viewLifecycleOwner,
            { updateShapeToMatch(it) }
        )

        gameViewModel.screenShapesLiveData.observe(
            viewLifecycleOwner,
            { updateShapesOnScreen(it) }
        )
        gameViewModel.timeLeftLiveData.observe(
            viewLifecycleOwner,
            { updateTimerText(it) }
        )

        gameViewModel.levelLiveData.observe(
            viewLifecycleOwner,
            { onLevelUpgrade(it) }
        )
    }

    private fun onLevelUpgrade(levelText: String) {
        levelTextView.text = levelText
        mainMenuButton.visible()
        nextLevelButton.visible()
    }

    private fun updateTimerText(secondsLeft: String) {
        timeLeftTextView.text = secondsLeft
    }

    private fun updateScoreText(score: String) {
        scoreTextView.text = score
    }

    private fun updateShapeToMatch(shape: Shape) =
        dragImageView.apply {
            setShape(requireContext(), shape)
        }

    private fun initListeners() {
        mainMenuButton.setOnClickListener { gameViewModel.restartGame(screenWidthAndHeight) }
        nextLevelButton.setOnClickListener { gameViewModel.startGame(screenWidthAndHeight) }

        dragImageView.setOnLongClickListener { v: View ->
            val item = ClipData.Item(v.tag as? CharSequence)
            val dragData = ClipData(
                v.tag as? CharSequence,
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item
            )

            val myShadow = MyDragShadowBuilder(dragImageView)
            val dragShadow = View.DragShadowBuilder(dragImageView)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                v.startDragAndDrop(dragData, dragShadow, null, 0);
            } else {
                v.startDrag(
                    dragData,
                    myShadow,
                    null,
                    0
                )
            }
        }

        val dragListen = View.OnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    dragImageView.invisible()
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }
                DragEvent.ACTION_DROP -> {
                    gameViewModel.handleMatchingShapeDrop(Pair(event.x.toInt(), event.y.toInt()))
                    v.invalidate()
                    true
                }
                else -> {
                    // An unknown action type was received.
                    false
                }
            }
        }
        containerView.setOnDragListener(dragListen)
    }

    private fun updateShapesOnScreen(shapes: List<Shape>) {
        clearPreviouslyConstructedShapes()

        for (shape in shapes) {
            ImageView(requireContext()).apply {
                setShape(requireContext(), shape)
                id = View.generateViewId()
                gameViewModel.addedViewIds.add(id)
                requestLayout()
                containerView.addView(this)
            }
        }
    }

    private fun clearPreviouslyConstructedShapes() {
        gameViewModel.addedViewIds.apply {
            forEach {
                containerView.apply {
                    Timber.d("Removing view with id: $it")
                    removeView(findViewById(it))
                }
            }
        }
    }

    private val screenWidthAndHeight: Pair<Int, Int>
        get() {
            val displayMetrics = DisplayMetrics()
            requireActivity().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display?.getRealMetrics(displayMetrics)
                } else {
                    windowManager!!.defaultDisplay.getMetrics(displayMetrics)
                }
            }

            displayMetrics.apply {
                return Pair(widthPixels, heightPixels)
            }
        }
}



