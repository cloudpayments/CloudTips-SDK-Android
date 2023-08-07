package ru.cloudtips.sdk.helpers

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commit()
}

fun FragmentActivity.addFragment(fragment: Fragment, frameId: Int, tag: String? = null) {
    supportFragmentManager.inTransaction { add(frameId, fragment, tag) }
}

fun FragmentActivity.addFragmentToBackStack(fragment: Fragment, frameId: Int, tag: String? = null) {
    supportFragmentManager.inTransaction { add(frameId, fragment, tag).addToBackStack(null) }
}

fun FragmentActivity.replaceFragment(fragment: Fragment, frameId: Int, tag: String? = null) {
    supportFragmentManager.inTransaction { replace(frameId, fragment, tag) }
}

fun FragmentActivity.addFragmentToBackStackWithTag(fragment: Fragment, frameId: Int, tag: String) {
    supportFragmentManager.inTransaction { add(frameId, fragment, tag).addToBackStack(null) }
}

fun FragmentActivity.replaceFragment(fragment: Fragment, frameId: Int) {
    supportFragmentManager.inTransaction { replace(frameId, fragment) }
}

fun FragmentActivity.replaceFragmentWithBackStack(fragment: Fragment, frameId: Int) {
    supportFragmentManager.inTransaction { replace(frameId, fragment).addToBackStack(null) }
}

fun FragmentActivity.cleanBackStack() {
    val fm = supportFragmentManager
    for (i in 0 until fm.backStackEntryCount) {
        fm.popBackStack()
    }
}

fun FragmentActivity.showBottomDialog(fragment: BottomSheetDialogFragment) {
    fragment.show(supportFragmentManager, "tag")
}

fun Fragment.showBottomDialog(fragment: BottomSheetDialogFragment) {
    fragment.show(childFragmentManager, "tag")
}