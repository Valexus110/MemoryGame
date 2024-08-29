package com.prjs.kotlin.memorygame.ui

import com.prjs.kotlin.memorygame.data.MockFirebaseRepository
import com.prjs.kotlin.memorygame.utils.FlowStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()
    private lateinit var mainViewModel: MainViewModel

    @Before
    fun setup() {
        mainViewModel = MainViewModel(repository = MockFirebaseRepository())
    }

    @Test
    fun downloadGame_isCompletedOrNot() = runTest {
        launch {
            mainViewModel.downloadGame("game2").collect { result ->
                assertTrue(result.first == FlowStatus.Error)
                assertTrue(result.second == null)
                cancel()
            }
        }
        launch {
            mainViewModel.downloadGame("game").collect { result ->
                assertTrue(result.first == FlowStatus.Success)
                assertTrue(result.second != null)
                cancel()
            }
        }
        launch {
            mainViewModel.downloadGame("game").collect { result ->
                assertTrue(result.first == FlowStatus.Error)
                assertTrue(result.second != null)
                cancel()
            }
        }
    }
    @Test
    fun saveDataToFirebase_isSavedOrNot() = runTest {
        launch {
            mainViewModel.saveDataToFirebase("game2").collect { result ->
                assertTrue(result == FlowStatus.Error)
                cancel()
            }
        }
        launch {
            mainViewModel.saveDataToFirebase("game").collect { result ->
                assertTrue(result == FlowStatus.Success)
                cancel()
            }
        }
    }

    @Test
    fun handleImageUploading_isHandledOrNot() = runTest {
        launch {
            mainViewModel.handleImageUploading("game2","path1", byteArrayOf()).collect { result ->
                assertTrue(result.first == "Exception")
                assertTrue(result.second == FlowStatus.Error)
                cancel()
            }
        }
        launch {
            mainViewModel.handleImageUploading("game","path", byteArrayOf()).collect { result ->
                assertTrue(result.first == "Result")
                assertTrue(result.second == FlowStatus.Success)
                cancel()
            }
        }
        launch {
            mainViewModel.handleImageUploading("game","path", byteArrayOf()).collect { result ->
                assertTrue(result.first == "Error")
                assertTrue(result.second == FlowStatus.Error)
                cancel()
            }
        }
    }

    @Test
    fun handleAllImagesUploaded_HandledOrNot() = runTest {
        launch {
            mainViewModel.handleAllImagesUploaded("game2", mutableListOf("")).collect { result ->
                assertTrue(result == FlowStatus.Error)
                cancel()
            }
        }
        launch {
            mainViewModel.handleAllImagesUploaded("game",mutableListOf("")).collect { result ->
                assertTrue(result == FlowStatus.Success)
                cancel()
            }
        }
    }
}