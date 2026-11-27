package ir.dakhlokharj

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Bootstrap() }
    }
}

@Composable
private fun Bootstrap() {
    Text(text = "دخل‌وخرج", fontSize = 24.sp)
}
