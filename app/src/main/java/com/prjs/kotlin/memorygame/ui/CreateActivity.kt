package com.prjs.kotlin.memorygame.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.prjs.kotlin.memorygame.MemoryGameApplication
import com.prjs.kotlin.memorygame.R
import com.prjs.kotlin.memorygame.adapters.ImagePickerAdapter
import com.prjs.kotlin.memorygame.databinding.ActivityCreateBinding
import com.prjs.kotlin.memorygame.utils.BoardSize
import com.prjs.kotlin.memorygame.utils.EXTRA_BOARD_SIZE
import com.prjs.kotlin.memorygame.utils.EXTRA_GAME_NAME
import com.prjs.kotlin.memorygame.utils.FlowStatus
import com.prjs.kotlin.memorygame.utils.getImageByteArray
import com.prjs.kotlin.memorygame.utils.isPermissionGranted
import kotlinx.coroutines.launch
import javax.inject.Inject

class CreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateBinding
    private lateinit var adapter: ImagePickerAdapter
    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private val chosenImageUris = MutableList<Uri?>(4) { _ -> null }
    private val uploadedImageUrls = mutableListOf<String>()
    private val urisSet = mutableSetOf<Int>()
    private var imagePosition = 0

    @Inject
    lateinit var viewModel: MainViewModel

    val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            // Handle permission requests results
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        (applicationContext as MemoryGameApplication).appComponent.inject(this)
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_BOARD_SIZE, BoardSize::class.java) as BoardSize
        } else {
            @Suppress("DEPRECATION") intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        }
        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = getString(R.string.create_title1, numImagesRequired)

        binding.apply {
            btnSave.setOnClickListener {
                lifecycleScope.launch {
                    saveDataToFirebase()
                }
            }
            etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
            etGameName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    btnSave.isEnabled = shouldEnableSaveButton()
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {

                }

            })

            adapter = addImagePickerAdapter()
            rvImagePicker.adapter = adapter
            rvImagePicker.setHasFixedSize(true)
            val spanCount = if (boardSize.getWidth() >= 4) 3 else 2
            rvImagePicker.layoutManager = GridLayoutManager(this@CreateActivity, spanCount)
        }
    }

    private fun addImagePickerAdapter(): ImagePickerAdapter {
        return ImagePickerAdapter(
            this@CreateActivity,
            chosenImageUris,
            boardSize,
            object : ImagePickerAdapter.ImageClickListener {
                override fun onPlaceHolderClicked(positionToReplace: Int) {
                    imagePosition = positionToReplace
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        isPermissionGranted(this@CreateActivity, READ_MEDIA_IMAGES)
                    ) {
                        launchIntentForPhotos()
                    } else if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                        isPermissionGranted(
                            this@CreateActivity,
                            READ_MEDIA_VISUAL_USER_SELECTED
                        )
                    ) {
                        launchIntentForPhotos()
                    } else if (isPermissionGranted(
                            this@CreateActivity,
                            READ_EXTERNAL_STORAGE
                        )
                    ) {
                        launchIntentForPhotos()
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            requestPermissions.launch(
                                arrayOf(
                                    READ_MEDIA_IMAGES,
                                    READ_MEDIA_VISUAL_USER_SELECTED
                                )
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES))
                        } else {
                            requestPermissions.launch(arrayOf(READ_EXTERNAL_STORAGE))
                        }
                    }
                }
            })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("NotifyDataSetChanged")
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            if (result.resultCode != Activity.RESULT_OK || data == null) {
                Log.w(TAG, "Did not get data back from the launched activity")
                return@registerForActivityResult
            }
            val selectedUri = data.data
            val clipData = data.clipData
            if (clipData != null) {
                Log.i(TAG, "clipData numImages ${clipData.itemCount}:$clipData")
                for (i in 0 until clipData.itemCount) {
                    val clipItem = clipData.getItemAt(i)
                    if (urisSet.size < numImagesRequired) {
                        chosenImageUris.add(clipItem.uri)
                    }
                }
            } else if (selectedUri != null) {
                Log.i(TAG, "data: $selectedUri")
                chosenImageUris[imagePosition] = selectedUri
                urisSet.add(imagePosition)
            }
            adapter.notifyDataSetChanged()
            supportActionBar?.title =
                getString(R.string.create_title2, urisSet.size, numImagesRequired)
            binding.btnSave.isEnabled = shouldEnableSaveButton()
        }

    private fun shouldEnableSaveButton(): Boolean {
        if (urisSet.size != numImagesRequired) {
            return false
        }
        binding.apply {
            if (etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_NAME_LENGTH) {
                return false
            }
        }
        return true

    }

    private suspend fun saveDataToFirebase() {
        Log.i(TAG, "saveDataToFirebase")
        binding.btnSave.isEnabled = false
        val customGameName = binding.etGameName.text.toString()
        val title = getString(R.string.create_title3)
        val message = getString(R.string.create_message2, customGameName)
        viewModel.saveDataToFirebase(customGameName).collect { response ->
            when (response) {
                FlowStatus.Success -> {
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
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
                    binding.btnSave.isEnabled = true
                }

                FlowStatus.HandleImages -> {
                    for (i in 0..<urisSet.size) {
                        lifecycleScope.launch {
                            chosenImageUris[i]?.let { handleImageUploading(customGameName, i, it) }
                        }
                    }
                }

                else -> {
                    Toast.makeText(this, getString(R.string.create_message3), Toast.LENGTH_LONG)
                        .show()
                    binding.btnSave.isEnabled = true
                }
            }
        }
    }

    private suspend fun handleImageUploading(gameName: String, index: Int, photoUri: Uri) {
        binding.pbUploading.visibility = View.VISIBLE
        val imageByteArray =
            getImageByteArray(photoUri = photoUri, contentResolver = contentResolver)
        val filePath = "images/$gameName/${System.currentTimeMillis()}-${index}.jpg"
        viewModel.handleImageUploading(gameName, filePath, imageByteArray).collect { response ->
            when (response.second) {
                FlowStatus.Success -> {
                    val downloadUrl = response.first
                    uploadedImageUrls.add(downloadUrl)
                    binding.pbUploading.progress =
                        uploadedImageUrls.size * 100 / chosenImageUris.size
                    Log.i(
                        TAG,
                        "Finished uploading $photoUri,num uploaded ${uploadedImageUrls.size}"
                    )
                    if (uploadedImageUrls.size == urisSet.size) {
                        handleAllImagesUploaded(gameName, uploadedImageUrls)
                    }
                }

                else -> {
                    Log.e(TAG, "Exception with Firebase Storage", Exception(response.first))
                    Toast.makeText(
                        this,
                        getString(R.string.create_message4),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.pbUploading.visibility = View.GONE
                }
            }
        }
    }

    private suspend fun handleAllImagesUploaded(
        gameName: String,
        imageUrls: MutableList<String>
    ) {
        binding.pbUploading.visibility = View.GONE
        viewModel.handleAllImagesUploaded(gameName, imageUrls).collect {
            when (it) {
                FlowStatus.Success -> {
                    Log.i(TAG, "Successfully create game $gameName")
                    val customTv = TextView(this)
                    customTv.text = getString(R.string.create_title4)
                    customTv.gravity = Gravity.CENTER
                    customTv.setPadding(16, 32, 16, 0)
                    customTv.textSize = 18f
                    customTv.minLines = 2
                    customTv.setTextColor(Color.BLACK)
                    customTv.setTypeface(null, Typeface.BOLD)
                    MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
                        .setCustomTitle(customTv)
                        .setPositiveButton("OK") { _, _ ->
                            val resultData = Intent()
                            resultData.putExtra(EXTRA_GAME_NAME, gameName)
                            setResult(Activity.RESULT_OK, resultData)
                            finish()
                        }.show()
                }

                else -> {
                    Toast.makeText(this, getString(R.string.create_message5), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        resultLauncher.launch(Intent.createChooser(intent, getString(R.string.create_title5)))
    }

    companion object {
        const val TAG = "CreateActivity"
        private const val READ_EXTERNAL_STORAGE = android.Manifest.permission.READ_EXTERNAL_STORAGE

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private const val READ_MEDIA_VISUAL_USER_SELECTED =
            android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private const val READ_MEDIA_IMAGES = android.Manifest.permission.READ_MEDIA_IMAGES
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14
    }
}