package com.rk.xededitor.ui.screens.settings.extensions

import android.app.Activity
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rk.extension.ExtensionManager
import com.rk.libcommons.DefaultScope
import com.rk.libcommons.application
import kotlinx.coroutines.launch
import org.robok.engine.core.components.compose.preferences.base.PreferenceGroup
import org.robok.engine.core.components.compose.preferences.base.PreferenceLayout
import org.robok.engine.core.components.compose.preferences.base.PreferenceTemplate
import java.io.File
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.rk.extension.Extension
import com.rk.libcommons.LoadingPopup
import com.rk.resources.getString
import com.rk.resources.strings
import com.rk.settings.PreferencesData
import com.rk.settings.PreferencesKeys
import com.rk.xededitor.App.Companion.getTempDir
import com.rk.xededitor.rkUtils
import com.rk.xededitor.ui.components.BottomSheetContent
import com.rk.xededitor.ui.components.SettingsToggle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStream

var selectedPlugin:Extension? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Extensions(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        var loading:LoadingPopup? = null
        runCatching {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (fileExtension == "plugin") {
                loading = LoadingPopup(context as Activity,null).show()
                loading?.setMessage("Installing...")
                DefaultScope.launch {
                    val pluginFile = File(application!!.getTempDir(),"installPlugin.plugin")
                    application!!.contentResolver.openInputStream(uri!!).use {
                        FileOutputStream(pluginFile).use { outputStream ->
                            it!!.copyTo(outputStream)
                        }
                    }
                    ExtensionManager.installPlugin(application!!,pluginFile)
                    pluginFile.delete()
                    delay(900)
                    withContext(Dispatchers.Main){
                        loading?.hide()
                    }
                }
            } else {
                rkUtils.toast("Selected file is not a .plugin file")
            }
        }.onFailure {
            loading?.hide()
            rkUtils.toast(it.message)
        }


    }

    PreferenceLayout(label = "Extensions", backArrowVisible = true) {
        val extensions = ExtensionManager.extensions
        val isLoaded = ExtensionManager.isLoaded
        val showPluginOptionSheet = remember { mutableStateOf(false) }



        LaunchedEffect("refreshPlugins") {
            launch {
                ExtensionManager.loadExistingPlugins(application!!)
            }
        }

        PreferenceGroup {
            PreferenceTemplate(
                modifier = modifier.clickable {
                    filePickerLauncher.launch(arrayOf("*/*"))
                },
                contentModifier = Modifier.fillMaxHeight(),
                title = { Text(text = "Install from storage") },
                enabled = true,
                applyPaddings = true,
                startWidget = {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = "Add Extensions")
                },
            )
        }

        PreferenceGroup(heading = "Extensions") {
            if (isLoaded.value){
                if (extensions.isEmpty()){
                    Text(
                        text = "No Extension Installed.",
                        modifier = Modifier.padding(16.dp)
                    )
                }else{
                    extensions.keys.forEach { plugin ->
                        SettingsToggle(
                            onLongClick = {
                                selectedPlugin = plugin
                                showPluginOptionSheet.value = true
                            },
                            label = plugin.name,
                            sideEffect = {},
                            key = "ext_"+plugin.packageName,
                        )
                    }
                }
            }else{
                Text(
                    text = "Loading...",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        val bottomSheetState = rememberModalBottomSheetState()

        if (showPluginOptionSheet.value){
            ModalBottomSheet(
                onDismissRequest = { showPluginOptionSheet.value = false }, sheetState = bottomSheetState
            ) {
                BottomSheetContent(buttons = {}) {
                    PreferenceGroup {
                        PreferenceTemplate(
                            modifier = modifier.clickable {
                                showPluginOptionSheet.value = false
                                DefaultScope.launch(Dispatchers.Main) {
                                    val loading = LoadingPopup(context as Activity,null).show()

                                    withContext(Dispatchers.Default){
                                        selectedPlugin?.let {
                                            ExtensionManager.deletePlugin(it)
                                        }
                                    }

                                    selectedPlugin = null
                                    delay(300)
                                    loading.hide()
                                }
                            },
                            contentModifier = Modifier.fillMaxHeight(),
                            title = { Text(text = "Delete") },
                            enabled = true,
                            applyPaddings = true,
                            startWidget = {
                                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete Extensions")
                            },
                        )
                    }
                }
            }
        }



    }
}
