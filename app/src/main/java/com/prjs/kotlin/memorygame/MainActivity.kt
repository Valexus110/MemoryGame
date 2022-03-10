package com.prjs.kotlin.memorygame

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prjs.kotlin.memorygame.models.BoardSize
import com.prjs.kotlin.memorygame.models.MemoryGame
import com.prjs.kotlin.memorygame.models.UserImageList
import com.prjs.kotlin.memorygame.utils.EXTRA_BOARD_SIZE
import com.prjs.kotlin.memorygame.utils.EXTRA_GAME_NAME
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_board_size.*
import kotlinx.android.synthetic.main.dialog_board_size.view.*
import kotlinx.android.synthetic.main.dialog_download_board.*


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 110
    }

    private val db = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages: List<String>? = null
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter
    private var boardSize: BoardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                if (memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()) {
                  //  val title = "Quit your current game?"
                    val title ="Завершить текущую игру?"
                    showAlertDialog(title, null) {
                        setupBoard()
                    }
                } else {
                    setupBoard()
                }
                return true
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGameName == null) {
                Log.e(TAG, "Got null custom game from CreateActivity")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    private fun showDownloadDialog() {
        val boardDownloadView =
            LayoutInflater.from(this).inflate(R.layout.dialog_download_board, null)
             //  val title = "Download custom game"
             val title ="Загрузить пользовательскую игру"
            showAlertDialog(title, boardDownloadView) {
                val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
                val gameToDownload = etDownloadGame.text.toString().trim()
                downloadGame(gameToDownload)
            }
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            val userImageList = document.toObject(UserImageList::class.java)
            if (userImageList?.images == null) {
                Log.e(TAG, "Invalid custom game data from Firestore")
                //  val text = "Sorry, we couldn't find any such game, '$customGameName'"
                val text ="Извините, подобной игры не существует, '$customGameName'"
                Snackbar.make(
                    clRoot,
                    text,
                    Snackbar.LENGTH_LONG
                ).show()
                return@addOnSuccessListener
            }
            val numCards = userImageList.images.size * 2
            boardSize = BoardSize.getByValue(numCards)
            customGameImages = userImageList.images
            for(imageUrl in userImageList.images) {
                Picasso.get().load(imageUrl).fetch()
            }
            //  val text = "You're now playing '$customGameName'!"
            val text ="Вы сейчас играете в '$customGameName'!"
            Snackbar.make(clRoot,text,Snackbar.LENGTH_LONG).show()
            gameName = customGameName
            setupBoard()
        }.addOnFailureListener { exeption ->
            Log.e(TAG, "Exception when retrieving game", exeption)
        }
    }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.radioGroup
      //  val title ="Choose sie of board"
        val title ="Выберите размер доски"
        showAlertDialog(title, boardSizeView) {
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbEasy2 -> BoardSize.EASY_2
                R.id.rbEasy3 -> BoardSize.EASY_3
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        }
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.radioGroup
        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.EASY_2 -> radioGroupSize.check(R.id.rbEasy2)
            BoardSize.EASY_3 -> radioGroupSize.check(R.id.rbEasy3)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        //  val title ="Choose new size"
        val title ="Выберите новый размер"
        showAlertDialog(title, boardSizeView) {
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
      //  val textNo ="Cancel"
        val textNo ="Отмена"
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton(textNo, null)
            .setPositiveButton("OK") { _, _ ->
                positiveClickListener.onClick(null)
            }.show()
    }


    private fun setupBoard() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        when (boardSize) {
            BoardSize.EASY -> {
                tvMoves.text = resources.getString(R.string.easy1_ru)
                tvPairs.text = resources.getString(R.string.pairs1_ru)
            }
            BoardSize.EASY_2 -> {
                tvMoves.text = resources.getString(R.string.easy2_ru)
                tvPairs.text = resources.getString(R.string.pairs2_ru)
            }
            BoardSize.EASY_3 -> {
                tvMoves.text = resources.getString(R.string.easy3_ru)
                tvPairs.text = resources.getString(R.string.pairs3_ru)
            }
            BoardSize.MEDIUM -> {
                tvMoves.text = resources.getString(R.string.medium_ru)
                tvPairs.text = resources.getString(R.string.pairs4_ru)
            }
            BoardSize.HARD -> {
                tvMoves.text = resources.getString(R.string.hard_ru)
                tvPairs.text = resources.getString(R.string.pairs5_ru)
            }
        }
        tvPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
        memoryGame = MemoryGame(boardSize, customGameImages)
        adapter = MemoryBoardAdapter(
            this,
            boardSize,
            memoryGame.cards,
            object : MemoryBoardAdapter.CardClickListener {
                override fun onCardClicked(position: Int) {
                    updateGameWithFlip(position)
                }

            })

        llGameInfo.setBackgroundColor(resources.getColor(R.color.info_color))
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
        rvBoard.setBackgroundColor(resources.getColor(R.color.board_color))
    }

    private fun updateGameWithFlip(position: Int) {
      //  val haveWon = "You already won!"
      //  val invalidMove = "Invalid move"
      //    val won = "You won!Congratulations"
          val haveWon = "Вы уже выиграли!"
          val invalidMove = "Неверный ход"
          val won ="Вы выиграли!Поздравляем"
        if (memoryGame.haveWonGame()) {
            Snackbar.make(clRoot, haveWon, Snackbar.LENGTH_LONG).show()
            return
        }
        if (memoryGame.isCardFaceUp(position)) {
            Snackbar.make(clRoot, invalidMove, Snackbar.LENGTH_SHORT).show()
            return
        }
        if (memoryGame.flipCard(position)) {
            Log.i(TAG, "Found a match!Num pairs found: ${memoryGame.numPairsFound}")
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full),
            ) as Int
            tvPairs.setTextColor(color)
            tvPairs.text = getString(R.string.pairs_ru,memoryGame.numPairsFound,boardSize.getNumPairs())
            if (memoryGame.haveWonGame()) {
                Snackbar.make(clRoot, won, Snackbar.LENGTH_LONG).show()
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.YELLOW,Color.BLUE,Color.CYAN)).oneShot()
            }
        }
        tvMoves.text = getString(R.string.moves_ru,memoryGame.getNumMoves())
        adapter.notifyDataSetChanged()
    }
}