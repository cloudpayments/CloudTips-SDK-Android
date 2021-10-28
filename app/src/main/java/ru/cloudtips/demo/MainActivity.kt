package ru.cloudtips.demo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ru.cloudtips.demo.databinding.ActivityMainBinding
import ru.cloudtips.sdk.CloudTipsSDK
import ru.cloudtips.sdk.CloudTipsSDK.TransactionStatus.Cancelled
import ru.cloudtips.sdk.CloudTipsSDK.TransactionStatus.Succeeded
import ru.cloudtips.sdk.TipsConfiguration
import ru.cloudtips.sdk.TipsData


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val launcher = CloudTipsSDK.getInstance().launcher(this){ status ->
            Toast.makeText(
                this, when (status) {
                    Succeeded -> "Чаевые получены"
                    Cancelled -> "Пользователь закрыл форму не оставив чаевых"
                }, Toast.LENGTH_SHORT
            ).show()
        }

        binding.buttonContinue.setOnClickListener {
            val phone = binding.editTextPhone.text.toString()

            if (phone.length != 12 || phone.take(2) != "+7") {
                Toast.makeText(this, R.string.main_phone_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tipsData = TipsData(phone, "CloudTips demo user", "partner_id")
            val configuration = TipsConfiguration(tipsData)
            //val configuration = TipsConfiguration(tipsData, true) // Режим тестирования
            launcher.launch(configuration)
        }
    }
}