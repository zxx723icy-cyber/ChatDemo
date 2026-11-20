package com.example.chatdemo

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class ContactItem(val name: String, val phones: List<String>)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppHolder.context = applicationContext
        setContent { ChatDemoApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDemoApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("聊天") }
    var activeChat by remember { mutableStateOf<String?>(null) }
    var contacts by remember { mutableStateOf<List<ContactItem>>(emptyList()) }
    var selectedPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var status by remember { mutableStateOf("数据仅在你确认后发送到配置的测试服务器。") }
    var showContactConfirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val contactPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contacts = readContacts()
            showContactConfirm = true
            status = "已读取 ${contacts.size} 条联系人记录，尚未上传。"
        } else status = "未获得通讯录权限；你仍可使用模拟聊天功能。"
    }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedPhotos = uris
        status = "已选择 ${uris.size} 张图片，尚未上传。"
    }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF07C160),
        secondary = Color(0xFF07C160),
        background = Color(0xFFF5F5F5)
    )) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        if (activeChat != null) {
                            IconButton(onClick = { activeChat = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    title = { Text(activeChat ?: if (tab == "聊天") "ChatDemo" else tab, fontWeight = FontWeight.SemiBold) },
                    actions = { Text("测试版", modifier = Modifier.padding(end = 16.dp), color = Color.Gray) }
                )
            },
            bottomBar = {
                if (activeChat == null) {
                    NavigationBar {
                        listOf("聊天" to Icons.Default.Chat, "通讯录" to Icons.Default.Contacts,
                            "发现" to Icons.Default.Explore, "我" to Icons.Default.Person).forEach { (label, icon) ->
                            NavigationBarItem(
                                selected = tab == label, onClick = { tab = label },
                                icon = { Icon(icon, null) }, label = { Text(label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5))) {
                if (activeChat != null) {
                    ChatRoom(chatName = activeChat!!)
                } else when (tab) {
                    "聊天" -> ChatList(onChatClick = { activeChat = it })
                    "通讯录" -> {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                    contacts = readContacts(); showContactConfirm = true
                                } else contactPermission.launch(Manifest.permission.READ_CONTACTS)
                            }) { Text("导入通讯录") }
                        }
                        Text("仅在你点击确认后上传；可先检查读取数量。", Modifier.padding(horizontal = 16.dp), color = Color.Gray)
                        LazyColumn {
                            items(contacts) { c ->
                                ListItem(headlineContent = { Text(c.name) }, supportingContent = { Text(c.phones.joinToString()) })
                                HorizontalDivider()
                            }
                        }
                    }
                    "发现" -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("相册测试", style = MaterialTheme.typography.titleLarge)
                        Text("通过系统照片选择器选择图片，不读取整个相册。")
                        Button(onClick = { photoPicker.launch("image/*") }) { Text("选择照片") }
                        Text("已选择 ${selectedPhotos.size} 张")
                        Button(enabled = selectedPhotos.isNotEmpty() && !busy, onClick = {
                            scope.launch {
                                busy = true
                                status = uploadPhotos(context, selectedPhotos)
                                busy = false
                            }
                        }) { Text(if (busy) "上传中…" else "确认上传所选照片") }
                    }
                    else -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ChatDemo 测试账号", style = MaterialTheme.typography.titleLarge)
                        Text("这是独立的聊天 UI 原型，不是微信官方应用。")
                        Text("数据处理：用户主动选择并确认后上传到你配置的测试服务器。")
                        Text("权限与数据清除请通过系统设置及测试服务器管理页处理。")
                    }
                }
                Text(status, Modifier.fillMaxWidth().padding(12.dp), color = Color.DarkGray)
            }
        }

        if (showContactConfirm) {
            AlertDialog(
                onDismissRequest = { showContactConfirm = false },
                title = { Text("确认上传通讯录？") },
                text = { Text("即将上传 ${contacts.size} 条联系人记录（姓名及电话号码）到你配置的测试服务器。请确保这些联系人已知情并同意。") },
                confirmButton = {
                    TextButton(onClick = {
                        showContactConfirm = false
                        scope.launch {
                            busy = true
                            status = uploadContacts(contacts)
                            busy = false
                        }
                    }) { Text("确认上传") }
                },
                dismissButton = { TextButton(onClick = { showContactConfirm = false }) { Text("取消") } }
            )
        }
    }
}

@Composable
fun ChatList(onChatClick: (String) -> Unit) {
    val demo = listOf(
        Triple("文件传输助手", "文件、图片和链接", "10:32"),
        Triple("产品测试群", "今天的版本已更新", "09:18"),
        Triple("小林（演示）", "收到，稍后查看", "昨天"),
        Triple("系统通知", "欢迎使用 ChatDemo", "周一")
    )
    LazyColumn {
        items(demo) { item ->
            ListItem(
                leadingContent = {
                    Box(Modifier.size(48.dp).background(Color(0xFFDDF3E5), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = Color(0xFF078A4B))
                    }
                },
                headlineContent = { Text(item.first, fontWeight = FontWeight.Medium) },
                supportingContent = { Text(item.second, maxLines = 1) },
                trailingContent = { Text(item.third, color = Color.Gray, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.clickable { onChatClick(item.first) }
            )
            HorizontalDivider(color = Color(0xFFEEEEEE))
        }
    }
}


@Composable
fun ChatRoom(chatName: String) {
    val messages = remember(chatName) {
        mutableStateListOf("你好，这里是本地演示聊天。")
    }
    var draft by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        message,
                        modifier = Modifier.background(Color(0xFFB9F6CA), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息") },
                maxLines = 4
            )
            IconButton(
                onClick = {
                    val message = draft.trim()
                    if (message.isNotEmpty()) {
                        messages.add(message)
                        draft = ""
                    }
                },
                enabled = draft.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "发送")
            }
        }
    }
}

fun readContacts(): List<ContactItem> {
    val resolver = AppHolder.context.contentResolver
    val result = mutableListOf<ContactItem>()
    val cursor = resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
        null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )
    cursor?.use {
        val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val phoneCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (it.moveToNext()) {
            val name = it.getString(nameCol) ?: "未命名"
            val phone = it.getString(phoneCol) ?: continue
            result.add(ContactItem(name, listOf(phone)))
        }
    }
    return result.distinctBy { it.name to it.phones.firstOrNull() }
}

object AppHolder { lateinit var context: android.content.Context }

suspend fun uploadContacts(contacts: List<ContactItem>): String = withContext(Dispatchers.IO) {
    try {
        val array = JSONArray()
        contacts.forEach { c ->
            array.put(JSONObject().put("name", c.name).put("phones", JSONArray(c.phones)))
        }
        val body = JSONObject().put("contacts", array).toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(BuildConfig.BASE_URL + "api/contacts")
            .header("X-Upload-Key", BuildConfig.UPLOAD_API_KEY).post(body).build()
        OkHttpClient().newCall(request).execute().use { if (it.isSuccessful) "通讯录上传成功。" else "上传失败：HTTP ${it.code}" }
    } catch (e: Exception) { "上传失败：${e.message}" }
}

suspend fun uploadPhotos(context: android.content.Context, uris: List<Uri>): String = withContext(Dispatchers.IO) {
    try {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        uris.forEachIndexed { index, uri ->
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                builder.addFormDataPart("photos", "selected_$index.jpg",
                    bytes.toRequestBody("application/octet-stream".toMediaType()))
            }
        }
        val request = Request.Builder().url(BuildConfig.BASE_URL + "api/photos")
            .header("X-Upload-Key", BuildConfig.UPLOAD_API_KEY).post(builder.build()).build()
        OkHttpClient().newCall(request).execute().use { if (it.isSuccessful) "照片上传成功。" else "上传失败：HTTP ${it.code}" }
    } catch (e: Exception) { "上传失败：${e.message}" }
}
