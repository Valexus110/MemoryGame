package com.prjs.kotlin.memorygame.ui

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.prjs.kotlin.memorygame.MemoryGameApplication
import com.prjs.kotlin.memorygame.R
import com.prjs.kotlin.memorygame.adapters.MemoryBoardAdapter
import com.prjs.kotlin.memorygame.databinding.ActivityMainBinding
import com.prjs.kotlin.memorygame.databinding.DialogBoardSizeBinding
import com.prjs.kotlin.memorygame.models.MemoryGame
import com.prjs.kotlin.memorygame.models.UserImageList
import com.prjs.kotlin.memorygame.utils.BoardSize
import com.prjs.kotlin.memorygame.utils.EXTRA_BOARD_SIZE
import com.prjs.kotlin.memorygame.utils.EXTRA_GAME_NAME
import com.prjs.kotlin.memorygame.utils.FlowStatus
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import javax.inject.Inject


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var boardSizeBinding: DialogBoardSizeBinding

    @Inject
    lateinit var viewModel: MainViewModel
    private var homeButton: MenuItem? = null
    private var gameName: String? = null
    private var customGameImages: List<String>? = null
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter
    private var boardSize: BoardSize = BoardSize.EASY
    private lateinit var analytics: FirebaseAnalytics
    override fun onCreate(savedInstanceState: Bundle?) {
        (applicationContext as MemoryGameApplication).appComponent.inject(this)
        super.onCreate(savedInstanceState)
        analytics = Firebase.analytics
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setupBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        homeButton = menu?.findItem(R.id.mi_home)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_home -> {
                showAlertDialog(getString(R.string.main_title5), null) {
                    gameName = null
                    customGameImages = null
                    homeButton?.setEnabled(false)
                    homeButton?.setIcon(R.drawable.ic_home_icon_inactive)
                    setupBoard()
                }

            }

            R.id.mi_refresh -> {
                if (memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()) {
                    showAlertDialog(getString(R.string.main_title1), null) {
                        setupBoard()
                    }
                } else {
                    setupBoard()
                }
                return true
            }

            R.id.mi_new_size -> {
                if (gameName != null) {
                    showAlertDialog(getString(R.string.main_title6), null) {
                        showNewSizeDialog()
                    }
                } else {
                    showNewSizeDialog()
                }
                return true
            }

            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }

            R.id.mi_download -> {
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
                if (customGameName == null) {
                    Log.e(TAG, "Got null custom game from CreateActivity")
                    return@registerForActivityResult
                } else lifecycleScope.launch {
                    downloadGame(customGameName)
                }
            }
        }


    private fun showDownloadDialog() {
        val boardDownloadView =
            View.inflate(this, R.layout.dialog_download_board, null)
        showAlertDialog(getString(R.string.main_title2), boardDownloadView) {
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            if (gameToDownload.isNotEmpty()) {
                lifecycleScope.launch {
                    downloadGame(gameToDownload, isDownload = true)
                }
            } else {
                Snackbar.make(
                    binding.clRoot,
                    "Field is empty",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun downloadGame(customGameName: String, isDownload: Boolean = false) {
        viewModel.downloadGame(customGameName).collect { response ->
            when (response.first) {
                FlowStatus.Success -> {
                    val userImageList = response.second!!
                    val numCards = userImageList.images!!.size * 2
                    if (isDownload) {
                        showAlertDialog(
                            getString(
                                R.string.main_title7,
                                customGameName,
                                4,
                                numCards / 4
                            ), null
                        ) {
                            setupDownloadGame(customGameName, userImageList, numCards)
                        }
                    } else {
                        setupDownloadGame(customGameName, userImageList, numCards)
                    }
                }

                else -> {
                    Log.e(TAG, "Invalid custom game data from Firestore")
                    Snackbar.make(
                        binding.clRoot,
                        getString(R.string.main_message1, customGameName),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupDownloadGame(
        customGameName: String,
        userImageList: UserImageList,
        numCards: Int
    ) {
        boardSize = BoardSize.getByValue(numCards)
        customGameImages = userImageList.images
        for (imageUrl in userImageList.images!!) {
            Picasso.get().load(imageUrl).fetch()
        }
        Snackbar.make(
            binding.clRoot,
            getString(R.string.main_message2, customGameName),
            Snackbar.LENGTH_LONG
        )
            .show()
        gameName = customGameName
        homeButton?.setEnabled(true)
        homeButton?.setIcon(R.drawable.ic_home_icon)
        setupBoard()
    }

    private fun showCreationDialog() {
        boardSizeBinding = DialogBoardSizeBinding.inflate(layoutInflater)
        val view = boardSizeBinding.root
        val radioGroupSize = boardSizeBinding.radioGroup
        showAlertDialog(getString(R.string.main_title3), view) {
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbEasy2 -> BoardSize.EASY_2
                R.id.rbEasy3 -> BoardSize.EASY_3
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            resultLauncher.launch(intent)
        }
    }

    private fun showNewSizeDialog() {
        boardSizeBinding = DialogBoardSizeBinding.inflate(layoutInflater)
        val view = boardSizeBinding.root
        val radioGroupSize = boardSizeBinding.radioGroup
        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.EASY_2 -> radioGroupSize.check(R.id.rbEasy2)
            BoardSize.EASY_3 -> radioGroupSize.check(R.id.rbEasy3)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog(getString(R.string.main_title4), view) {
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbEasy2 -> BoardSize.EASY_2
                R.id.rbEasy3 -> BoardSize.EASY_3
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            setupBoard()
        }
    }

    private fun showAlertDialog(
        title: String,
        view: View?,
        positiveClickListener: View.OnClickListener
    ) {
        val customTv = TextView(this)
        customTv.text = title
        customTv.gravity = Gravity.CENTER
        customTv.setPadding(16, 32, 16, 0)
        customTv.textSize = 18f
        customTv.minLines = 2
        customTv.setTextColor(Color.BLACK)
        customTv.setTypeface(null, Typeface.BOLD)
        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
            .setCustomTitle(customTv)
            .setView(view)
            .setNegativeButton(getString(R.string.main_message3), null)
            .setPositiveButton("OK") { _, _ ->
                positiveClickListener.onClick(null)
            }.show()
    }

    private fun setupBoard() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        binding.apply {
            when (boardSize) {
                BoardSize.EASY -> {
                    tvMoves.text = resources.getString(R.string.easy1)
                    tvPairs.text = resources.getString(R.string.pairs1)
                }

                BoardSize.EASY_2 -> {
                    tvMoves.text = resources.getString(R.string.easy2)
                    tvPairs.text = resources.getString(R.string.pairs2)
                }

                BoardSize.EASY_3 -> {
                    tvMoves.text = resources.getString(R.string.easy3)
                    tvPairs.text = resources.getString(R.string.pairs3)
                }

                BoardSize.MEDIUM -> {
                    tvMoves.text = resources.getString(R.string.medium)
                    tvPairs.text = resources.getString(R.string.pairs4)
                }

                BoardSize.HARD -> {
                    tvMoves.text = resources.getString(R.string.hard)
                    tvPairs.text = resources.getString(R.string.pairs5)
                }
            }
            tvPairs.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.color_progress_none
                )
            )
            memoryGame = MemoryGame(boardSize, customGameImages)
            adapter = MemoryBoardAdapter(
                this@MainActivity,
                boardSize,
                memoryGame.cards,
                object : MemoryBoardAdapter.CardClickListener {
                    override fun onCardClicked(position: Int) {
                        updateGameWithFlip(position)
                    }

                })

            llGameInfo.setBackgroundColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.info_color
                )
            )
            rvBoard.adapter = adapter
            rvBoard.setHasFixedSize(true)
            rvBoard.layoutManager = GridLayoutManager(this@MainActivity, boardSize.getWidth())
            rvBoard.setBackgroundColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.info_color
                )
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateGameWithFlip(position: Int) {
        binding.apply {
            if (memoryGame.haveWonGame()) {
                Snackbar.make(clRoot, getString(R.string.have_won), Snackbar.LENGTH_LONG).show()
                return
            }
            if (memoryGame.isCardFaceUp(position)) {
                Snackbar.make(clRoot, getString(R.string.invalid_move), Snackbar.LENGTH_SHORT)
                    .show()
                return
            }
            if (memoryGame.flipCard(position)) {
                Log.i(TAG, "Found a match!Num pairs found: ${memoryGame.numPairsFound}")
                val color = ArgbEvaluator().evaluate(
                    memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                    ContextCompat.getColor(this@MainActivity, R.color.color_progress_none),
                    ContextCompat.getColor(this@MainActivity, R.color.color_progress_full),
                ) as Int
                tvPairs.setTextColor(color)
                tvPairs.text =
                    getString(R.string.pairs, memoryGame.numPairsFound, boardSize.getNumPairs())
                if (memoryGame.haveWonGame()) {
                    Snackbar.make(clRoot, getString(R.string.won), Snackbar.LENGTH_LONG).show()
                    CommonConfetti.rainingConfetti(
                        clRoot,
                        intArrayOf(Color.YELLOW, Color.BLUE, Color.CYAN)
                    ).oneShot()
                }
            }
            tvMoves.text = getString(R.string.moves, memoryGame.getNumMoves())
            adapter.notifyDataSetChanged()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}