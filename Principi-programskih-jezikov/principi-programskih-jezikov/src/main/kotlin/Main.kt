import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import api.openweather.OpenWeatherApi
import org.litote.kmongo.text
import api.*
import models.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import org.litote.kmongo.set
import org.litote.kmongo.setTo
import org.litote.kmongo.updateOneById
import java.lang.reflect.Array.set


private val DarkColorPalette = darkColors(
    primary = Color(0xFFBB86FC),
    primaryVariant = Color(0xFF3700B3),
    secondary = Color(0xFF03DAC6),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)


fun main() = application {
Window(onCloseRequest = ::exitApplication, title= "Windsync") {
    App()
}
}
@Composable
fun App( ) {
    MaterialTheme(colors=DarkColorPalette) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkColorPalette.background // to je Color(0xFF121212)
        ) {
            var selectedScreen by remember { mutableStateOf("Users") }
            Row(modifier = Modifier.fillMaxSize()) {
                NavPanel(
                    onNavigate = { selectedScreen = it }
                )
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when (selectedScreen) {
                        "Add user" -> AddUserScreen()
                        "Users" -> UsersScreen()
                        "Scraper" -> ScraperScreen()
                        "Generator" -> GeneratorScreen()
                        "About" -> AboutScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationButton(label: String, icon: ImageVector, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick(label) }
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, color = Color.White)
        }
    }
}


@Composable
fun NavPanel(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(Color(0xFF1A1A1A)), // sidebar barva
        verticalArrangement = Arrangement.Top
    ) {
        NavigationButton("Add user", Icons.Filled.Add, onNavigate)
        NavigationButton("Users", Icons.Default.Menu, onNavigate)
        Divider(
            color = Color(0xFF333333),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        NavigationButton("Scraper", Icons.Default.Share, onNavigate)
        NavigationButton("Generator", Icons.Default.Build, onNavigate)

        Spacer(modifier = Modifier.weight(1f))

        NavigationButton("About", Icons.Default.Info, onNavigate)
    }
}


@Composable
fun UsersScreen() {
    val userList = remember { mutableStateListOf<User>() }

    LaunchedEffect(Unit) {
        val fromDb = Database.users.find().toList()
        userList.clear()
        userList.addAll(fromDb)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(userList) { user ->
                UserCard(user = user, onUserUpdated = { updatedUser ->
                    val index = userList.indexOfFirst { it._id == updatedUser._id }
                    if (index != -1) {
                        userList[index] = updatedUser
                    }
                })
            }
        }
    }
}



@Composable
fun UserCard(user: User, onUserUpdated: (User) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(user.username) }
    var editedEmail by remember { mutableStateOf(user.email) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(4.dp)
            .clickable { isEditing = true },
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color(0xFF2A2A2A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color(0xFF2A2A2A))
                .onFocusChanged {
                    if (isEditing && !it.hasFocus) {
                        isEditing = false
                    }
                }
                .focusRequester(focusRequester)
                .focusable(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )

            if (isEditing) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Ime") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editedEmail,
                    onValueChange = { editedEmail = it },
                    label = { Text("Email") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(onClick = {
                        Database.users.updateOneById(user._id, set(
                            User::username setTo editedName,
                            User::email setTo editedEmail
                        ))
                        onUserUpdated(user.copy(username = editedName, email = editedEmail))
                        isEditing = false
                    }) {
                        Text("save")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        editedName = user.username
                        editedEmail = user.email
                        isEditing = false
                    }) {
                        Text("cancel")
                    }
                }
            } else {
                Text(user.username, color = Color.White, fontSize = 18.sp)
                Text(user.email, color = Color.LightGray, fontSize = 14.sp)
            }
        }
    }
}





@Composable
fun ScraperScreen() {}

@Composable
fun GeneratorScreen() {}

@Composable
fun AboutScreen() {}

@Composable
fun AddUserScreen(){}