package net.syarihu.licensescribe.screen

import java.io.File

/**
 * Generates OpenSourceLicensesActivity code using View-based UI (no Compose dependency).
 */
class OpenSourceLicensesActivityGenerator {

  fun generate(
    packageName: String,
    licensesClassName: String,
    activityClassName: String,
    outputDir: File,
    resOutputDir: File,
    nightMode: String,
  ) {
    val packageDir = File(outputDir, packageName.replace('.', '/'))
    packageDir.mkdirs()

    val activityFile = File(packageDir, "$activityClassName.kt")
    activityFile.writeText(generateActivityCode(packageName, licensesClassName, activityClassName, nightMode))

    // Generate layout XML
    val layoutDir = File(resOutputDir, "layout")
    layoutDir.mkdirs()
    val layoutFile = File(layoutDir, "licensescribe_recycler_view.xml")
    layoutFile.writeText(generateLayoutXml())
  }

  /**
   * Generate layout XML for RecyclerView.
   *
   * RecyclerView is defined in XML to enable scrollbar display.
   * Programmatically created RecyclerView cannot show scrollbars properly
   * because ScrollBarDrawable is not initialized from theme.
   */
  private fun generateLayoutXml(): String = """
<?xml version="1.0" encoding="utf-8"?>
<androidx.recyclerview.widget.RecyclerView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/licensescribe_recycler_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:scrollbars="vertical" />
  """.trimIndent()

  private fun generateActivityCode(
    packageName: String,
    licensesClassName: String,
    activityClassName: String,
    nightMode: String,
  ): String {
    return if (nightMode == "no") {
      generateLegacyActivityCode(packageName, licensesClassName, activityClassName)
    } else {
      generateDarkModeActivityCode(packageName, licensesClassName, activityClassName, nightMode)
    }
  }

  /**
   * Generate legacy Activity code without dark mode support.
   */
  private fun generateLegacyActivityCode(
    packageName: String,
    licensesClassName: String,
    activityClassName: String,
  ): String = """
package $packageName

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.syarihu.licensescribe.LicenseInfo
import $packageName.R

/**
 * Activity that displays a list of open source licenses.
 *
 * Use [start] to launch this activity with optional customization.
 */
class $activityClassName : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    val themeColor = intent.getIntExtra(EXTRA_THEME_COLOR, -1).takeIf { it != -1 }
    val toolbarTitleColor = intent.getIntExtra(EXTRA_TOOLBAR_TITLE_COLOR, -1).takeIf { it != -1 }
    val title = intent.getStringExtra(EXTRA_TITLE) ?: DEFAULT_TITLE

    // Determine if toolbar background is light or dark
    val isLightBackground = themeColor?.let { ColorUtils.calculateLuminance(it) > 0.5 } ?: true

    // Set status bar icon color based on toolbar background brightness
    WindowCompat.getInsetsController(window, window.decorView).apply {
      isAppearanceLightStatusBars = isLightBackground
    }

    // Auto-determine title color if not specified
    val resolvedTitleColor = toolbarTitleColor
      ?: if (isLightBackground) Color.BLACK else Color.WHITE

    val rootLayout = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      )
    }

    val toolbar = Toolbar(this).apply {
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
      )
      minimumHeight = getActionBarSize()
      setTitle(title)
      themeColor?.let { setBackgroundColor(it) }
      setTitleTextColor(resolvedTitleColor)
    }
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    // Tint navigation icon after setting up action bar
    toolbar.navigationIcon?.colorFilter = PorterDuffColorFilter(resolvedTitleColor, PorterDuff.Mode.SRC_IN)

    // Accent color for license text (use theme color if specified, otherwise default)
    val accentColor = themeColor ?: DEFAULT_THEME_COLOR

    // Inflate RecyclerView from XML to get proper scrollbar support
    val recyclerView = LayoutInflater.from(this)
      .inflate(R.layout.licensescribe_recycler_view, null) as RecyclerView
    recyclerView.apply {
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        0,
        1f,
      )
      layoutManager = LinearLayoutManager(context)
      clipToPadding = false
      adapter = LicenseAdapter(
        accentColor = accentColor,
        onItemClick = { license ->
          license.artifactUrl?.let { url ->
            openUrl(url)
          }
        },
        onLicenseClick = { license ->
          license.licenseUrl?.let { url ->
            openUrl(url)
          }
        },
      )
    }

    // Handle WindowInsets for edge-to-edge
    ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(top = systemBars.top)
      insets
    }

    ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { view, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(bottom = systemBars.bottom)
      insets
    }

    (recyclerView.adapter as LicenseAdapter).submitList($licensesClassName.all)

    rootLayout.addView(toolbar)
    rootLayout.addView(recyclerView)
    setContentView(rootLayout)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        finish()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun getActionBarSize(): Int {
    val typedValue = TypedValue()
    return if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
      TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
    } else {
      dpToPx(56)
    }
  }

  private fun dpToPx(dp: Int): Int =
    TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      dp.toFloat(),
      resources.displayMetrics,
    ).toInt()

  private fun openUrl(url: String) {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
      Toast.makeText(this, "No application found to open URL", Toast.LENGTH_SHORT).show()
    }
  }

  private class LicenseAdapter(
    private val accentColor: Int,
    private val onItemClick: (LicenseInfo) -> Unit,
    private val onLicenseClick: (LicenseInfo) -> Unit,
  ) : ListAdapter<LicenseInfo, LicenseAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
      ViewHolder(LicenseItemView(parent.context, accentColor))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      holder.bind(getItem(position), onItemClick, onLicenseClick)
    }

    class ViewHolder(private val licenseItemView: LicenseItemView) : RecyclerView.ViewHolder(licenseItemView) {
      fun bind(
        license: LicenseInfo,
        onItemClick: (LicenseInfo) -> Unit,
        onLicenseClick: (LicenseInfo) -> Unit,
      ) {
        licenseItemView.bind(license, onLicenseClick)
        licenseItemView.setOnClickListener { onItemClick(license) }
      }
    }

    private object DiffCallback : DiffUtil.ItemCallback<LicenseInfo>() {
      override fun areItemsTheSame(oldItem: LicenseInfo, newItem: LicenseInfo): Boolean =
        oldItem.artifactId == newItem.artifactId

      override fun areContentsTheSame(oldItem: LicenseInfo, newItem: LicenseInfo): Boolean =
        oldItem == newItem
    }
  }

  private class LicenseItemView(context: Context, private val accentColor: Int) : LinearLayout(context) {
    private val titleView: TextView
    private val artifactIdView: TextView
    private val copyrightView: TextView
    private val licenseView: TextView

    init {
      orientation = VERTICAL
      layoutParams = MarginLayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT,
      ).apply {
        val margin = dpToPx(16)
        val verticalMargin = dpToPx(8)
        setMargins(margin, verticalMargin, margin, verticalMargin)
      }
      val padding = dpToPx(16)
      setPadding(padding, padding, padding, padding)

      // Card background with rounded corners
      val cardBackground = GradientDrawable().apply {
        setColor(CARD_BACKGROUND_COLOR)
        cornerRadius = dpToPx(8).toFloat()
      }

      // Add ripple effect to card
      val rippleColor = ColorStateList.valueOf(RIPPLE_COLOR)
      background = RippleDrawable(rippleColor, cardBackground, cardBackground)
      isClickable = true
      isFocusable = true

      elevation = dpToPx(4).toFloat()

      titleView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTextColor(TEXT_PRIMARY_COLOR)
        maxLines = 1
      }

      artifactIdView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(TEXT_SECONDARY_COLOR)
        maxLines = 1
      }

      copyrightView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(TEXT_SECONDARY_COLOR)
      }

      licenseView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(accentColor)
        layoutParams = LayoutParams(
          LayoutParams.WRAP_CONTENT,
          LayoutParams.WRAP_CONTENT,
        ).apply {
          topMargin = dpToPx(8)
        }
        // Add ripple effect to license text (gray ripple)
        val licenseRippleColor = ColorStateList.valueOf(RIPPLE_COLOR)
        val mask = GradientDrawable().apply {
          setColor(Color.WHITE) // Mask needs a color to define ripple bounds
          cornerRadius = dpToPx(4).toFloat()
        }
        background = RippleDrawable(licenseRippleColor, null, mask)
        val horizontalPadding = dpToPx(4)
        val verticalPadding = dpToPx(2)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        isClickable = true
        isFocusable = true
      }

      addView(titleView)
      addView(artifactIdView)
      addView(copyrightView)
      addView(licenseView)
    }

    fun bind(license: LicenseInfo, onLicenseClick: (LicenseInfo) -> Unit) {
      titleView.text = license.artifactName
      artifactIdView.text = license.artifactId
      copyrightView.apply {
        text = license.copyrightHolders.joinToString(", ")
        visibility = if (license.copyrightHolders.isEmpty()) View.GONE else View.VISIBLE
      }
      licenseView.apply {
        text = license.licenseName
        if (license.licenseUrl != null) {
          setOnClickListener { onLicenseClick(license) }
          isClickable = true
          isFocusable = true
        } else {
          setOnClickListener(null)
          isClickable = false
          isFocusable = false
        }
      }
    }

    private fun dpToPx(dp: Int): Int =
      TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics,
      ).toInt()

    companion object {
      private const val CARD_BACKGROUND_COLOR = 0xFFFFFFFF.toInt()
      private const val TEXT_PRIMARY_COLOR = 0xFF212121.toInt()
      private const val TEXT_SECONDARY_COLOR = 0xFF757575.toInt()
      private const val RIPPLE_COLOR = 0x20000000
    }
  }

  companion object {
    private const val EXTRA_THEME_COLOR = "extra_theme_color"
    private const val EXTRA_TOOLBAR_TITLE_COLOR = "extra_toolbar_title_color"
    private const val EXTRA_TITLE = "extra_title"
    private const val DEFAULT_TITLE = "Open Source Licenses"
    private const val DEFAULT_THEME_COLOR = 0xFF6200EE.toInt()

    /**
     * Start the open source licenses activity.
     *
     * @param context The context to start the activity from
     * @param themeColor Optional theme color used for toolbar background and license text (as Color int)
     * @param toolbarTitleColor Optional title text color for the toolbar (as Color int)
     * @param title Optional custom title (default: "Open Source Licenses")
     */
    @JvmStatic
    fun start(
      context: Context,
      themeColor: Int? = null,
      toolbarTitleColor: Int? = null,
      title: String = DEFAULT_TITLE,
    ) {
      val intent = Intent(context, $activityClassName::class.java).apply {
        themeColor?.let { putExtra(EXTRA_THEME_COLOR, it) }
        toolbarTitleColor?.let { putExtra(EXTRA_TOOLBAR_TITLE_COLOR, it) }
        putExtra(EXTRA_TITLE, title)
      }
      context.startActivity(intent)
    }
  }
}
  """.trimIndent()

  /**
   * Generate Activity code with dark mode support.
   */
  private fun generateDarkModeActivityCode(
    packageName: String,
    licensesClassName: String,
    activityClassName: String,
    nightMode: String,
  ): String {
    val isDarkModeExpression = when (nightMode) {
      "yes" -> "true"
      else -> "isSystemDarkMode()" // "followSystem" mode
    }

    val darkModeHelperMethod = if (nightMode == "followSystem") {
      """
  private fun isSystemDarkMode(): Boolean {
    val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
  }
"""
    } else {
      ""
    }

    val configurationImport = if (nightMode == "followSystem") {
      "import android.content.res.Configuration"
    } else {
      ""
    }

    return """
package $packageName

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
$configurationImport
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.syarihu.licensescribe.LicenseInfo
import $packageName.R

/**
 * Activity that displays a list of open source licenses.
 *
 * Use [start] to launch this activity with optional customization.
 */
class $activityClassName : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    val themeColor = intent.getIntExtra(EXTRA_THEME_COLOR, -1).takeIf { it != -1 }
    val toolbarTitleColor = intent.getIntExtra(EXTRA_TOOLBAR_TITLE_COLOR, -1).takeIf { it != -1 }
    val title = intent.getStringExtra(EXTRA_TITLE) ?: DEFAULT_TITLE

    val isDarkMode = $isDarkModeExpression

    // Determine if toolbar background is light or dark
    val isLightBackground = themeColor?.let { ColorUtils.calculateLuminance(it) > 0.5 } ?: !isDarkMode

    // Set status bar icon color based on toolbar background brightness
    WindowCompat.getInsetsController(window, window.decorView).apply {
      isAppearanceLightStatusBars = isLightBackground
    }

    // Auto-determine title color if not specified
    val resolvedTitleColor = toolbarTitleColor
      ?: if (isLightBackground) Color.BLACK else Color.WHITE

    val rootLayout = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      )
      setBackgroundColor(if (isDarkMode) BACKGROUND_COLOR_DARK else BACKGROUND_COLOR_LIGHT)
    }

    val toolbar = Toolbar(this).apply {
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
      )
      minimumHeight = getActionBarSize()
      setTitle(title)
      themeColor?.let { setBackgroundColor(it) }
        ?: setBackgroundColor(if (isDarkMode) TOOLBAR_COLOR_DARK else TOOLBAR_COLOR_LIGHT)
      setTitleTextColor(resolvedTitleColor)
    }
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    // Tint navigation icon after setting up action bar
    toolbar.navigationIcon?.colorFilter = PorterDuffColorFilter(resolvedTitleColor, PorterDuff.Mode.SRC_IN)

    // Accent color for license text (use theme color if specified, otherwise default)
    val accentColor = themeColor ?: DEFAULT_THEME_COLOR

    // Inflate RecyclerView from XML to get proper scrollbar support
    val recyclerView = LayoutInflater.from(this)
      .inflate(R.layout.licensescribe_recycler_view, null) as RecyclerView
    recyclerView.apply {
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        0,
        1f,
      )
      layoutManager = LinearLayoutManager(context)
      clipToPadding = false
      setBackgroundColor(if (isDarkMode) BACKGROUND_COLOR_DARK else BACKGROUND_COLOR_LIGHT)
      adapter = LicenseAdapter(
        accentColor = accentColor,
        isDarkMode = isDarkMode,
        onItemClick = { license ->
          license.artifactUrl?.let { url ->
            openUrl(url)
          }
        },
        onLicenseClick = { license ->
          license.licenseUrl?.let { url ->
            openUrl(url)
          }
        },
      )
    }

    // Handle WindowInsets for edge-to-edge
    ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(top = systemBars.top)
      insets
    }

    ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { view, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(bottom = systemBars.bottom)
      insets
    }

    (recyclerView.adapter as LicenseAdapter).submitList($licensesClassName.all)

    rootLayout.addView(toolbar)
    rootLayout.addView(recyclerView)
    setContentView(rootLayout)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        finish()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun getActionBarSize(): Int {
    val typedValue = TypedValue()
    return if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
      TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
    } else {
      dpToPx(56)
    }
  }

  private fun dpToPx(dp: Int): Int =
    TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      dp.toFloat(),
      resources.displayMetrics,
    ).toInt()

  private fun openUrl(url: String) {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
      Toast.makeText(this, "No application found to open URL", Toast.LENGTH_SHORT).show()
    }
  }
$darkModeHelperMethod
  private class LicenseAdapter(
    private val accentColor: Int,
    private val isDarkMode: Boolean,
    private val onItemClick: (LicenseInfo) -> Unit,
    private val onLicenseClick: (LicenseInfo) -> Unit,
  ) : ListAdapter<LicenseInfo, LicenseAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
      ViewHolder(LicenseItemView(parent.context, accentColor, isDarkMode))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      holder.bind(getItem(position), onItemClick, onLicenseClick)
    }

    class ViewHolder(private val licenseItemView: LicenseItemView) : RecyclerView.ViewHolder(licenseItemView) {
      fun bind(
        license: LicenseInfo,
        onItemClick: (LicenseInfo) -> Unit,
        onLicenseClick: (LicenseInfo) -> Unit,
      ) {
        licenseItemView.bind(license, onLicenseClick)
        licenseItemView.setOnClickListener { onItemClick(license) }
      }
    }

    private object DiffCallback : DiffUtil.ItemCallback<LicenseInfo>() {
      override fun areItemsTheSame(oldItem: LicenseInfo, newItem: LicenseInfo): Boolean =
        oldItem.artifactId == newItem.artifactId

      override fun areContentsTheSame(oldItem: LicenseInfo, newItem: LicenseInfo): Boolean =
        oldItem == newItem
    }
  }

  private class LicenseItemView(
    context: Context,
    private val accentColor: Int,
    private val isDarkMode: Boolean,
  ) : LinearLayout(context) {
    private val titleView: TextView
    private val artifactIdView: TextView
    private val copyrightView: TextView
    private val licenseView: TextView

    init {
      orientation = VERTICAL
      layoutParams = MarginLayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT,
      ).apply {
        val margin = dpToPx(16)
        val verticalMargin = dpToPx(8)
        setMargins(margin, verticalMargin, margin, verticalMargin)
      }
      val padding = dpToPx(16)
      setPadding(padding, padding, padding, padding)

      // Card background with rounded corners
      val cardBackground = GradientDrawable().apply {
        setColor(if (isDarkMode) CARD_BACKGROUND_COLOR_DARK else CARD_BACKGROUND_COLOR_LIGHT)
        cornerRadius = dpToPx(8).toFloat()
      }

      // Add ripple effect to card
      val rippleColor = ColorStateList.valueOf(if (isDarkMode) RIPPLE_COLOR_DARK else RIPPLE_COLOR_LIGHT)
      background = RippleDrawable(rippleColor, cardBackground, cardBackground)
      isClickable = true
      isFocusable = true

      elevation = dpToPx(4).toFloat()

      titleView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTextColor(if (isDarkMode) TEXT_PRIMARY_COLOR_DARK else TEXT_PRIMARY_COLOR_LIGHT)
        maxLines = 1
      }

      artifactIdView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(if (isDarkMode) TEXT_SECONDARY_COLOR_DARK else TEXT_SECONDARY_COLOR_LIGHT)
        maxLines = 1
      }

      copyrightView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(if (isDarkMode) TEXT_SECONDARY_COLOR_DARK else TEXT_SECONDARY_COLOR_LIGHT)
      }

      licenseView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(accentColor)
        layoutParams = LayoutParams(
          LayoutParams.WRAP_CONTENT,
          LayoutParams.WRAP_CONTENT,
        ).apply {
          topMargin = dpToPx(8)
        }
        // Add ripple effect to license text (gray ripple)
        val licenseRippleColor = ColorStateList.valueOf(if (isDarkMode) RIPPLE_COLOR_DARK else RIPPLE_COLOR_LIGHT)
        val mask = GradientDrawable().apply {
          setColor(Color.WHITE) // Mask needs a color to define ripple bounds
          cornerRadius = dpToPx(4).toFloat()
        }
        background = RippleDrawable(licenseRippleColor, null, mask)
        val horizontalPadding = dpToPx(4)
        val verticalPadding = dpToPx(2)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        isClickable = true
        isFocusable = true
      }

      addView(titleView)
      addView(artifactIdView)
      addView(copyrightView)
      addView(licenseView)
    }

    fun bind(license: LicenseInfo, onLicenseClick: (LicenseInfo) -> Unit) {
      titleView.text = license.artifactName
      artifactIdView.text = license.artifactId
      copyrightView.apply {
        text = license.copyrightHolders.joinToString(", ")
        visibility = if (license.copyrightHolders.isEmpty()) View.GONE else View.VISIBLE
      }
      licenseView.apply {
        text = license.licenseName
        if (license.licenseUrl != null) {
          setOnClickListener { onLicenseClick(license) }
          isClickable = true
          isFocusable = true
        } else {
          setOnClickListener(null)
          isClickable = false
          isFocusable = false
        }
      }
    }

    private fun dpToPx(dp: Int): Int =
      TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics,
      ).toInt()

    companion object {
      // Light mode colors
      private const val CARD_BACKGROUND_COLOR_LIGHT = 0xFFFFFFFF.toInt()
      private const val TEXT_PRIMARY_COLOR_LIGHT = 0xFF212121.toInt()
      private const val TEXT_SECONDARY_COLOR_LIGHT = 0xFF757575.toInt()
      private const val RIPPLE_COLOR_LIGHT = 0x20000000

      // Dark mode colors
      private const val CARD_BACKGROUND_COLOR_DARK = 0xFF1E1E1E.toInt()
      private const val TEXT_PRIMARY_COLOR_DARK = 0xFFE0E0E0.toInt()
      private const val TEXT_SECONDARY_COLOR_DARK = 0xFFB0B0B0.toInt()
      private const val RIPPLE_COLOR_DARK = 0x20FFFFFF
    }
  }

  companion object {
    private const val EXTRA_THEME_COLOR = "extra_theme_color"
    private const val EXTRA_TOOLBAR_TITLE_COLOR = "extra_toolbar_title_color"
    private const val EXTRA_TITLE = "extra_title"
    private const val DEFAULT_TITLE = "Open Source Licenses"
    private const val DEFAULT_THEME_COLOR = 0xFF6200EE.toInt()

    // Background colors
    private const val BACKGROUND_COLOR_LIGHT = 0xFFF5F5F5.toInt()
    private const val BACKGROUND_COLOR_DARK = 0xFF121212.toInt()
    private const val TOOLBAR_COLOR_LIGHT = 0xFFFFFFFF.toInt()
    private const val TOOLBAR_COLOR_DARK = 0xFF1E1E1E.toInt()

    /**
     * Start the open source licenses activity.
     *
     * @param context The context to start the activity from
     * @param themeColor Optional theme color used for toolbar background and license text (as Color int)
     * @param toolbarTitleColor Optional title text color for the toolbar (as Color int)
     * @param title Optional custom title (default: "Open Source Licenses")
     */
    @JvmStatic
    fun start(
      context: Context,
      themeColor: Int? = null,
      toolbarTitleColor: Int? = null,
      title: String = DEFAULT_TITLE,
    ) {
      val intent = Intent(context, $activityClassName::class.java).apply {
        themeColor?.let { putExtra(EXTRA_THEME_COLOR, it) }
        toolbarTitleColor?.let { putExtra(EXTRA_TOOLBAR_TITLE_COLOR, it) }
        putExtra(EXTRA_TITLE, title)
      }
      context.startActivity(intent)
    }
  }
}
    """.trimIndent()
  }
}
