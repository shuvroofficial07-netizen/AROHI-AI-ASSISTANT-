package com.example.engine

data class ExecutionVerification(
    val isSuccess: Boolean,
    val summary: String,
    val details: String? = null
)

class VerificationEngine {

    /**
     * [foregroundVerified] is null when Android gave us no way to confirm which app is in the
     * foreground (no Accessibility connection). In that case we say so instead of claiming success.
     */
    fun verifyAppLaunch(
        success: Boolean,
        appLabel: String,
        foregroundVerified: Boolean? = null
    ): ExecutionVerification {
        return if (success) {
            when (foregroundVerified) {
                true -> ExecutionVerification(
                    isSuccess = true,
                    summary = "$appLabel অ্যাপটি চালু হয়েছে এবং এখন সামনে আছে (verified)।"
                )
                false -> ExecutionVerification(
                    isSuccess = false,
                    summary = "$appLabel চালু করার অনুরোধ পাঠিয়েছি, কিন্তু অ্যাপটি সামনে এসেছে বলে যাচাই করতে পারিনি।"
                )
                null -> ExecutionVerification(
                    isSuccess = true,
                    summary = "$appLabel চালু করার অনুরোধ Android গ্রহণ করেছে। (Accessibility ছাড়া foreground যাচাই করা সম্ভব নয়।)"
                )
            }
        } else {
            ExecutionVerification(
                isSuccess = false,
                summary = "$appLabel অ্যাপটি চালু করা সম্ভব হয়নি। অ্যাপটি আপনার ফোনে ইনস্টল আছে কিনা যাচাই করুন।"
            )
        }
    }

    fun verifyCall(success: Boolean, target: String, hadDirectPermission: Boolean): ExecutionVerification {
        return if (success) {
            if (hadDirectPermission) {
                ExecutionVerification(
                    isSuccess = true,
                    summary = "$target-কে সরাসরি কল করা হচ্ছে।"
                )
            } else {
                ExecutionVerification(
                    isSuccess = true,
                    summary = "$target-এর জন্য ডায়ালার ওপেন করা হয়েছে।"
                )
            }
        } else {
            ExecutionVerification(
                isSuccess = false,
                summary = "$target-কে কল করার ডায়ালার ওপেন করা যায়নি।"
            )
        }
    }

    fun verifyTorch(success: Boolean, enabled: Boolean): ExecutionVerification {
        return if (success) {
            val stateText = if (enabled) "চালু" else "বন্ধ"
            ExecutionVerification(
                isSuccess = true,
                summary = "ফ্ল্যাশলাইট সফলভাবে $stateText করা হয়েছে।"
            )
        } else {
            ExecutionVerification(
                isSuccess = false,
                summary = "ফ্ল্যাশলাইট পরিবর্তন করা যায়নি। ক্যামেরা হার্ডওয়্যার পারমিশন চেক করুন।"
            )
        }
    }

    fun verifyVolume(success: Boolean, percent: Int): ExecutionVerification {
        return if (success) {
            ExecutionVerification(
                isSuccess = true,
                summary = "মিডিয়া ভলিউম $percent%-এ সেট করা হয়েছে।"
            )
        } else {
            ExecutionVerification(
                isSuccess = false,
                summary = "ভলিউম সেট করা সম্ভব হয়নি।"
            )
        }
    }

    fun verifyMemory(savedId: Long, key: String): ExecutionVerification {
        return if (savedId > 0) {
            ExecutionVerification(
                isSuccess = true,
                summary = "'$key' সফলভাবে মেমোরিতে সংরক্ষণ করা হয়েছে।"
            )
        } else {
            ExecutionVerification(
                isSuccess = false,
                summary = "মেমোরিতে সেভ করতে সমস্যা হয়েছে।"
            )
        }
    }
}
