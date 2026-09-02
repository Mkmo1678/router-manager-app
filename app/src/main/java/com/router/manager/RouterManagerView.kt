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
import android.widget.GridLayout
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

    private val cardsContainer: FrameLayout
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
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33000000"))
            }
            setOnClickListener { onSettingsClick?.invoke() }
        }

        // 布局快捷切换按钮
        val layoutBtn = ImageView(context).apply {
            val size = dp(40)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(10)
            }
            setImageResource(R.drawable.ic_layout)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33000000"))
            }
            setOnClickListener {
                val current = AppSettings.getHomeLayout(context)
                val next = (current + 1) % 4
                AppSettings.setHomeLayout(context, next)
                val names = arrayOf("列表布局", "紧凑列表", "网格布局", "大图标")
                android.widget.Toast.makeText(context, "切换为${names[next]}", android.widget.Toast.LENGTH_SHORT).show()
                refresh()
            }
        }

        titleBar.addView(titleText)
        titleBar.addView(layoutBtn)
        titleBar.addView(settingsBtn)
        contentLayout.addView(titleBar)

        cardsContainer = FrameLayout(context)
        contentLayout.addView(cardsContainer)

        scrollView.addView(contentLayout)
        addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        applyTheme()
        applyBackground()
    }

    fun refresh() {
        routers = RouterStore.loadRouters(context)
        titleText.text = AppSettings.getTitle(context)
        themeColor = AppSettings.getThemeColor(context)
        cardsContainer.removeAllViews()
        val layout = AppSettings.getHomeLayout(context)
        when (layout) {
            AppSettings.LAYOUT_COMPACT -> {
                val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                for (router in routers) list.addView(createCompactCard(router))
                cardsContainer.addView(list)
            }
            AppSettings.LAYOUT_GRID -> {
                val grid = createGridLayout(2)
                for (router in routers) grid.addView(createGridCard(router))
                cardsContainer.addView(grid)
            }
            AppSettings.LAYOUT_ICON -> {
                val grid = createGridLayout(3)
                for (router in routers) grid.addView(createIconCard(router))
                cardsContainer.addView(grid)
            }
            else -> { // LAYOUT_LIST
                val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                for (router in routers) list.addView(createRouterCard(router))
                cardsContainer.addView(list)
            }
        }
    }

    private fun createGridLayout(spanCount: Int): GridLayout {
        return GridLayout(context).apply {
            columnCount = spanCount
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
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
        val hasCustomIcon = router.customIconPath != null && File(router.customIconPath).exists()
        val iconContainer = FrameLayout(context).apply {
            val size = dp(58)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(14)
            }
            if (!hasCustomIcon) {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(router.iconColor)
                }
            }
            elevation = dp(4).toFloat()
        }

        val icon = ImageView(context).apply {
            if (hasCustomIcon) {
                // 自定义图标：填满整个圆形区域，无颜色背景
                val size = dp(58)
                layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(BitmapFactory.decodeFile(router.customIconPath))
                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
            } else {
                // 默认图标：颜色背景 + 白色路由器图标
                val size = dp(28)
                layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
                setImageResource(R.drawable.ic_launcher_foreground)
                setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
            }
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
                ViewGroup.LayoutParams.WRAP_CONTENT,
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

        // 编辑按钮（半透明圆圈+白色铅笔，统一风格）
        val editBtn = ImageView(context).apply {
            val size = dp(28)
            layoutParams = LinearLayout.LayoutParams(size, size)
            setImageResource(R.drawable.ic_edit)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#55000000"))
            }
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

    // ─── 紧凑列表卡片 ────────────────────────────────────────
    private fun createCompactCard(router: RouterStore.Router): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setColor(Color.parseColor("#D8FFFFFF"))
                setStroke(dp(1), Color.parseColor("#22FFFFFF"))
            }
            elevation = dp(3).toFloat()
            setOnClickListener { onRouterClick?.invoke(router) }
        }

        // 图标
        val hasCustomIcon = router.customIconPath != null && File(router.customIconPath).exists()
        val icon = ImageView(context).apply {
            val size = dp(42)
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = dp(12) }
            scaleType = ImageView.ScaleType.CENTER_CROP
            if (hasCustomIcon) {
                setImageBitmap(BitmapFactory.decodeFile(router.customIconPath))
                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(v: android.view.View, o: android.graphics.Outline) {
                        o.setOval(0, 0, v.width, v.height)
                    }
                }
            } else {
                setImageResource(R.drawable.ic_launcher_foreground)
                setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(router.iconColor)
                }
                setPadding(dp(8), dp(8), dp(8), dp(8))
            }
        }

        // 文字区
        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        // 名称行：名称 + 小分段控件
        val nameRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val nameText = TextView(context).apply {
            text = router.name
            textSize = 15f
            setTextColor(Color.parseColor("#1A1A1A"))
            setTypeface(null, Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val isRemote = router.accessMode == RouterStore.ACCESS_REMOTE && router.remoteUrl.isNotEmpty()
        // 小分段控件：本地 / 远程
        val modeSegment = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(2), dp(2), dp(2), dp(2))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(6).toFloat()
                setColor(Color.parseColor("#E0E0E0"))
            }
            layoutParams = LinearLayout.LayoutParams(dp(84), dp(24)).apply {
                marginStart = dp(6)
            }
        }
        val localBtn = TextView(context).apply {
            text = "本地"
            textSize = 10f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = dp(2)
            }
            if (router.accessMode == RouterStore.ACCESS_LOCAL) {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(4).toFloat()
                    setColor(Color.WHITE)
                }
                setTextColor(themeColor)
            } else {
                setTextColor(Color.parseColor("#888888"))
            }
            setOnClickListener { toggleAccessMode(router, RouterStore.ACCESS_LOCAL) }
        }
        val remoteBtn = TextView(context).apply {
            text = "远程"
            textSize = 10f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            if (isRemote) {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(4).toFloat()
                    setColor(Color.WHITE)
                }
                setTextColor(0xFFE65100.toInt())
            } else {
                setTextColor(Color.parseColor("#888888"))
            }
            setOnClickListener { toggleAccessMode(router, RouterStore.ACCESS_REMOTE) }
        }
        modeSegment.addView(localBtn)
        modeSegment.addView(remoteBtn)

        nameRow.addView(nameText)
        nameRow.addView(modeSegment)

        val urlText = TextView(context).apply {
            text = if (router.showIp) router.currentUrl else "••••••••••••"
            textSize = 12f
            setTextColor(Color.parseColor("#777777"))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(3), 0, 0)
        }
        textLayout.addView(nameRow)
        textLayout.addView(urlText)

        // 编辑按钮（精致小铅笔）
        val editBtn = ImageView(context).apply {
            val size = dp(26)
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginStart = dp(8) }
            setImageResource(R.drawable.ic_edit)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
            setPadding(dp(5), dp(5), dp(5), dp(5))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#55000000"))
            }
            setOnClickListener { onRouterEdit?.invoke(router) }
        }

        card.addView(icon)
        card.addView(textLayout)
        card.addView(editBtn)
        return card
    }

    // ─── 网格卡片（2列） ────────────────────────────────────────
    private fun createGridCard(router: RouterStore.Router): View {
        val card = FrameLayout(context).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(5), dp(5), dp(5), dp(5))
            }
            setOnClickListener { onRouterClick?.invoke(router) }
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(16), dp(12), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20).toFloat()
                setColor(Color.parseColor("#D8FFFFFF"))
                setStroke(dp(1), Color.parseColor("#22FFFFFF"))
            }
            elevation = dp(3).toFloat()
        }

        // 图标
        val hasCustomIcon = router.customIconPath != null && File(router.customIconPath).exists()
        val icon = ImageView(context).apply {
            val size = dp(56)
            layoutParams = LinearLayout.LayoutParams(size, size)
            scaleType = ImageView.ScaleType.CENTER_CROP
            if (hasCustomIcon) {
                setImageBitmap(BitmapFactory.decodeFile(router.customIconPath))
                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(v: android.view.View, o: android.graphics.Outline) {
                        o.setOval(0, 0, v.width, v.height)
                    }
                }
            } else {
                setImageResource(R.drawable.ic_launcher_foreground)
                setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(router.iconColor)
                }
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }
        }

        val nameText = TextView(context).apply {
            text = router.name
            textSize = 14f
            setTextColor(Color.parseColor("#1A1A1A"))
            setTypeface(null, Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(10), 0, 0)
        }

        val isRemote = router.accessMode == RouterStore.ACCESS_REMOTE && router.remoteUrl.isNotEmpty()
        // 分段控件：本地 / 远程
        val modeSegment = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(2), dp(2), dp(2), dp(2))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(7).toFloat()
                setColor(Color.parseColor("#E0E0E0"))
            }
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(26)).apply {
                topMargin = dp(8)
            }
        }
        val localBtn = TextView(context).apply {
            text = "本地"
            textSize = 10f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = dp(2)
            }
            if (router.accessMode == RouterStore.ACCESS_LOCAL) {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(5).toFloat()
                    setColor(Color.WHITE)
                }
                setTextColor(themeColor)
            } else {
                setTextColor(Color.parseColor("#888888"))
            }
            setOnClickListener { toggleAccessMode(router, RouterStore.ACCESS_LOCAL) }
        }
        val remoteBtn = TextView(context).apply {
            text = "远程"
            textSize = 10f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            if (isRemote) {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(5).toFloat()
                    setColor(Color.WHITE)
                }
                setTextColor(0xFFE65100.toInt())
            } else {
                setTextColor(Color.parseColor("#888888"))
            }
            setOnClickListener { toggleAccessMode(router, RouterStore.ACCESS_REMOTE) }
        }
        modeSegment.addView(localBtn)
        modeSegment.addView(remoteBtn)

        content.addView(icon)
        content.addView(nameText)
        content.addView(modeSegment)

        // 编辑按钮（半透明圆圈+白色铅笔，清晰可见）
        val editBtn = ImageView(context).apply {
            val size = dp(28)
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.END).apply {
                setMargins(0, dp(6), dp(6), 0)
            }
            setImageResource(R.drawable.ic_edit)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#55000000"))
            }
            setOnClickListener { onRouterEdit?.invoke(router) }
        }

        card.addView(content)
        card.addView(editBtn)
        return card
    }

    // ─── 大图标卡片（3列） ────────────────────────────────────────
    private fun createIconCard(router: RouterStore.Router): View {
        val card = FrameLayout(context).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            setOnClickListener { onRouterClick?.invoke(router) }
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(2), dp(4), dp(2), dp(4))
        }

        val hasCustomIcon = router.customIconPath != null && File(router.customIconPath).exists()
        val icon = ImageView(context).apply {
            val size = dp(60)
            layoutParams = LinearLayout.LayoutParams(size, size)
            scaleType = ImageView.ScaleType.CENTER_CROP
            if (hasCustomIcon) {
                setImageBitmap(BitmapFactory.decodeFile(router.customIconPath))
                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(v: android.view.View, o: android.graphics.Outline) {
                        o.setOval(0, 0, v.width, v.height)
                    }
                }
            } else {
                setImageResource(R.drawable.ic_launcher_foreground)
                setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(router.iconColor)
                }
                setPadding(dp(13), dp(13), dp(13), dp(13))
            }
            elevation = dp(4).toFloat()
        }

        val nameText = TextView(context).apply {
            text = router.name
            textSize = 12f
            setTextColor(Color.parseColor("#1A1A1A"))
            setTypeface(null, Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(6), 0, 0)
        }

        val isRemote = router.accessMode == RouterStore.ACCESS_REMOTE && router.remoteUrl.isNotEmpty()
        // 小分段控件：本地 / 远程
        val modeSegment = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(1), dp(1), dp(1), dp(1))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(5).toFloat()
                setColor(Color.parseColor("#E0E0E0"))
            }
            layoutParams = LinearLayout.LayoutParams(dp(76), dp(20)).apply {
                topMargin = dp(4)
            }
        }
        val localBtn = TextView(context).apply {
            text = "本地"
            textSize = 9f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = dp(1)
            }
            if (router.accessMode == RouterStore.ACCESS_LOCAL) {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(4).toFloat()
                    setColor(Color.WHITE)
                }
                setTextColor(themeColor)
            } else {
                setTextColor(Color.parseColor("#888888"))
            }
            setOnClickListener { toggleAccessMode(router, RouterStore.ACCESS_LOCAL) }
        }
        val remoteBtn = TextView(context).apply {
            text = "远程"
            textSize = 9f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            if (isRemote) {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(4).toFloat()
                    setColor(Color.WHITE)
                }
                setTextColor(0xFFE65100.toInt())
            } else {
                setTextColor(Color.parseColor("#888888"))
            }
            setOnClickListener { toggleAccessMode(router, RouterStore.ACCESS_REMOTE) }
        }
        modeSegment.addView(localBtn)
        modeSegment.addView(remoteBtn)

        content.addView(icon)
        content.addView(nameText)
        content.addView(modeSegment)

        // 编辑按钮（半透明圆圈+白色铅笔，统一风格）
        val editBtn = ImageView(context).apply {
            val size = dp(26)
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.END).apply {
                setMargins(0, dp(2), dp(2), 0)
            }
            setImageResource(R.drawable.ic_edit)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
            setPadding(dp(5), dp(5), dp(5), dp(5))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#55000000"))
            }
            setOnClickListener { onRouterEdit?.invoke(router) }
        }

        card.addView(content)
        card.addView(editBtn)
        return card
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
