package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.ArohiEmotion
import com.example.engine.EmotionEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Arohi AI Assistant", appName)
  }

  @Test
  fun `test emotion engine text inference`() {
    val emotionEngine = EmotionEngine()
    assertEquals(ArohiEmotion.IDLE, emotionEngine.currentEmotion.value)
    
    val happyEmotion = emotionEngine.inferEmotionFromText("দারুণ হয়েছে!")
    assertEquals(ArohiEmotion.HAPPY, happyEmotion)

    val errorEmotion = emotionEngine.inferEmotionFromText("দুঃখিত, ব্যর্থ হয়েছে")
    assertEquals(ArohiEmotion.ERROR, errorEmotion)
  }
}
