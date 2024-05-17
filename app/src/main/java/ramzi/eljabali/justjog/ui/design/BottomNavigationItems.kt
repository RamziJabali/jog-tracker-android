package ramzi.eljabali.justjog.ui.design

import androidx.annotation.DrawableRes
import ramzi.eljabali.justjog.R

enum class BottomNavigationItems(title: String, @DrawableRes val icon: Int) {
    STATISTICS("statistics", R.drawable.front_arrow),
    CALENDAR("calendar", R.drawable.back_arrow),

}