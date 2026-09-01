package com.router.manager

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

/**
 * 首页：路由器列表（玻璃拟态卡片风格）
 * 每卡片独立IP/密码显示隐藏，支持保存密码自动填充
 */
class RouterManagerView(context: Context, private val statusBarHeight: Int) : FrameLayout(context) {

    var onRouterClick: ((RouterStore.Router) -> Unit)? = null
    var onRouterEdit: ((RouterStore.Router) -> Unit)? = null
    var onRouterDelete: ((RouterStore.Router) -> Unit)? = null
    var onSettingsClick: (() -> Unit)? = null

    private val cardsContainer: LinearLayout
    private val titleText: TextView
    private val settingsBtn: ImageView
    private val contentLayout: LinearLayout
    private val bgImageView: ImageView
    private var routers: MutableList<RouterStore.Router> = mutableListOf()
    private var themeColor: Int = AppSettings.defaultTheme

    init {
        bgImageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            visibility = View.GONE
        }
        addView(bgImageView)

        val scrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
        }

        contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), statusBarHeight + dp(12), dp(16), dp(140))
        }

        val titleBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(8), dp(6), dp(24))
        }

        titleText = TextView(context).apply {
            text = AppSettings.getTitle(context)
            textSize = 30f
            setTextColor(Color.parseColor("#1A1A1A"))
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        settingsBtn = ImageView(context).apply {
            val size = dp(40)
            layoutParams = LinearLayout.LayoutParams(size, size)
            setImageResource(R.drawable.ic_settings)
            setColorFilter(Color.parseColor("#999999"), PorterDuff.Mode.SRC_ATOP)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#22000000"))
            }
            setOnClickListener { onSettingsClick?.invoke() }
        }

        titleBar.addView(titleText)
        titleBar.addView(settingsBtn)
        contentLayout.addView(titleBar)

        cardsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        contentLayout.addView(cardsContainer)

        scrollView.addView(contentLayout)
        addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        applyTheme()
        applyBackground()
    }

    fun refresh() {
        routers = RouterStore.loadRouters(context)
        titleText.text = AppSettings.getTitle(context)
        cardsContainer.removeAllViews()
        for (router in routers) {
            cardsContainer.addView(createRouterCard(router))
        }
    }

    fun applyTheme() {
        themeColor = AppSettings.getThemeColor(context)
        applyTextColor()
    }

    private fun applyTextColor() {
        val dark = AppSettings.isDarkBackground(context)
        if (dark) {
            titleText.setTextColor(Color.WHITE)
            settingsBtn.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        } else {
            titleText.setTextColor(Color.parseColor("#1A1A1A"))
            settingsBtn.setColorFilter(themeColor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    fun applyBackground() {
        val path = AppSettings.getBackgroundPath(context)
        if (path != null && File(path).exists()) {
            val bitmap = BitmapFactory.decodeFile(path)
            if (bitmap != null) {
                bgImageView.setImageBitmap(bitmap)
                bgImageView.visibility = View.VISIBLE
                setBackgroundColor(Color.TRANSPARENT)
                contentLayout.setBackgroundColor(Color.TRANSPARENT)
                applyTextColor()
                return
            }
        }
        bgImageView.setImageDrawable(null)
        bgImageView.visibility = View.GONE
        setBackgroundColor(Color.parseColor("#E8EAED"))
        applyTextColor()
    }

    private fun createRouterCard(router: RouterStore.Router): View {
        val show = router.showIp
        val hasCredentials = router.username.isNotEmpty() || router.password.isNotEmpty()

        // 外层卡片（玻璃拟态：半透明 + 边框 + 圆角 + 阴影，宽度稍窄）
        val card = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(8)
                marginEnd = dp(8)
                bottomMargin = dp(16)
            }
            elevation = dp(8).toFloat()
        }

        // 玻璃背景层（高不透明度确保文字清晰）
        val glassBg = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(22).toFloat()
                setColor(Color.parseColor("#D8FFFFFF")) // 85% 不透明白，玻璃感+清晰
                setStroke(dp(1), Color.parseColor("#80FFFFFF")) // 半透明白边框
            }
        }

        // 内容区
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(18), dp(12), dp(18))
        }

        // 圆形图标
        val iconContainer = FrameLayout(context).apply {
            val size = dp(58)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(14)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(router.iconColor)
            }
            elevation = dp(4).toFloat()
        }

        val icon = ImageView(context).apply {
            val size = dp(28)
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            setImageResource(R.drawable.ic_launcher_foreground)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }
        if (router.customIconPath != null && File(router.customIconPath).exists()) {
            icon.setImageBitmap(BitmapFactory.decodeFile(router.customIconPath))
            icon.colorFilter = null
        }
        iconContainer.addView(icon)

        // 右侧文字区
        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        // 名称行：名称独占整行，超长自动换行确保完整显示
        val nameRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val nameText = TextView(context).apply {
            text = router.name
            textSize = 18f
            setTextColor(Color.parseColor("#1A1A1A"))
            setTypeface(null, Typeface.BOLD)
            maxLines = 2
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        nameRow.addView(nameText)

        val isRemote = router.accessMode == RouterStore.ACCESS_REMOTE && router.remoteUrl.isNotEmpty()

        // 分段控件行：本地 / 远程（与编辑对话框一致的风格）
        val modeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, 0)
        }

        // 分段控件：本地 / 远程
        val modeSegment = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(2), dp(2), dp(2), dp(2))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#E0E0E0"))
            }
            layoutParams = LinearLayout.LayoutParams(
                dp(112), dp(28)
            )
        }

        val localBtn = TextView(context).apply {
            text = "本地"
            textSize = 11f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = dp(2)
            }
            if (router.accessMode == RouterStore.ACCESS_LOCAL) {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(6).toFloat()
                    setColor(Color.WHITE)
                }
                setTextColor(themeColor)
            } else {
                setTextColor(Color.parseColor("#888888"))
            }
            setOnClickListener {
                toggleAccessMode(router, RouterStore.ACCESS_LOCAL)
            }
        }

        val remoteBtn = TextView(context).apply {
            text = "远程"
            textSize = 11f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            if (isRemote) {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(6).toFloat()
                    setColor(Color.WHITE)
                }
                setTextColor(0xFFE65100.toInt())
            } else {
                setTextColor(Color.parseColor("#888888"))
            }
            setOnClickListener {
                toggleAccessMode(router, RouterStore.ACCESS_REMOTE)
            }
        }

        modeSegment.addView(localBtn)
        modeSegment.addView(remoteBtn)

        nameRow.addView(nameText)
        modeRow.addView(modeSegment)

        val urlText = TextView(context).apply {
            text = if (show) router.currentUrl else "地址：••••••••••••"
            textSize = 13f
            setTextColor(Color.parseColor("#555555"))
            setPadding(0, dp(3), 0, 0)
        }

        textLayout.addView(nameRow)
        textLayout.addView(modeRow)
        textLayout.addView(urlText)

        // 账号密码行（如果有保存）
        if (hasCredentials) {
            val credText = TextView(context).apply {
                val user = if (show && router.username.isNotEmpty()) router.username else "••••"
                val pwd = if (show && router.password.isNotEmpty()) router.password else "••••"
                text = "账号：$user  密码：$pwd"
                textSize = 12f
                setTextColor(Color.parseColor("#777777"))
                setPadding(0, dp(6), 0, 0)
            }
            textLayout.addView(credText)
        }

        // 进入管理文字
        val enterText = TextView(context).apply {
            text = "进入管理 →"
            textSize = 13f
            setTextColor(themeColor)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(8), 0, 0)
        }
        textLayout.addView(enterText)

        // 右侧按钮区：眼睛 + 编辑
        val btnLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                dp(40), ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // 眼睛按钮（切换IP/密码显示隐藏）- 自定义矢量图标
        val eyeBtn = ImageView(context).apply {
            val size = dp(34)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                bottomMargin = dp(8)
            }
            setImageResource(if (show) R.drawable.ic_eye else R.drawable.ic_eye_off)
            setColorFilter(Color.parseColor("#555555"), PorterDuff.Mode.SRC_ATOP)
            setPadding(dp(7), dp(7), dp(7), dp(7))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#22000000"))
            }
            setOnClickListener {
                toggleShowIp(router)
            }
        }

        // 编辑按钮
        val editBtn = ImageView(context).apply {
            val size = dp(32)
            layoutParams = LinearLayout.LayoutParams(size, size)
            setImageResource(R.drawable.ic_edit)
            setColorFilter(Color.parseColor("#AAAAAA"), PorterDuff.Mode.SRC_ATOP)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setOnClickListener { onRouterEdit?.invoke(router) }
        }

        btnLayout.addView(eyeBtn)
        btnLayout.addView(editBtn)

        content.addView(iconContainer)
        content.addView(textLayout)
        content.addView(btnLayout)

        card.addView(glassBg)
        card.addView(content)

        card.setOnClickListener { onRouterClick?.invoke(router) }
        card.setOnLongClickListener {
            onRouterDelete?.invoke(router)
            true
        }

        return card
    }

    /** 切换单个路由器的IP/密码显示状态并保存 */
    private fun toggleShowIp(router: RouterStore.Router) {
        val index = routers.indexOfFirst { it.id == router.id }
        if (index >= 0) {
            val updated = router.copy(showIp = !router.showIp)
            routers[index] = updated
            RouterStore.saveRouters(context, routers)
            refresh()
        }
    }

    /** 切换单个路由器的访问模式（本地/远程）并保存 */
    private fun toggleAccessMode(router: RouterStore.Router, targetMode: Int) {
        val index = routers.indexOfFirst { it.id == router.id }
        if (index >= 0) {
            if (targetMode == RouterStore.ACCESS_REMOTE && router.remoteUrl.isEmpty()) {
                android.widget.Toast.makeText(context, "请先在编辑中填写远程地址", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            val updated = router.copy(accessMode = targetMode)
            routers[index] = updated
            RouterStore.saveRouters(context, routers)
            refresh()
            android.widget.Toast.makeText(
                context,
                "已切换为${if (targetMode == RouterStore.ACCESS_REMOTE) "远程" else "本地"}访问",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
