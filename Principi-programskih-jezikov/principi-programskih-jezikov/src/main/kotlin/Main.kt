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


import models.Location
import models.Station
import scraper.thewindpower.Scraper
import datamodels.ScrapedWindFarm
import kotlinx.coroutines.*


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
fun ScraperScreen() {
    val countries = listOf(
        "Slovenia" to "country_windfarms_en_100_slovenia.php",
        "Austria" to "country_windfarms_en_13_austria.php",
        "Albanija" to "country_windfarms_en_75_albania.php",
        "Hrvaška" to "country_windfarms_en_45_croatia.php"
    )

    var selectedCountry by remember { mutableStateOf(countries.first()) }
    var expanded by remember { mutableStateOf(false) }
    val windFarms = remember { mutableStateListOf<ScrapedWindFarm>() }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCountry) {
        isLoading = true

        delay(2000)
        val scraped = Scraper.scrapeWindFarms(selectedCountry.second)
        windFarms.clear()
        windFarms.addAll(scraped)

        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Država: ${selectedCountry.first}")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                countries.forEach { country ->
                    DropdownMenuItem(
                        onClick = {
                            selectedCountry = country
                            expanded = false
                        }
                    ) {
                        Text(text = country.first)
                    }
                }
            }
        }
        Text(
            text = "Vetrnice v ${selectedCountry.first}",
            style = MaterialTheme.typography.h5,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 200.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                items(windFarms) { farm ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth(),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = Color(0xFF2A2A2A)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = farm.name, style = MaterialTheme.typography.h6, color = Color.White)
                            Text(
                                text = "Lokacija: ${farm.location.getLatitude()}, ${farm.location.getLongitude()}",
                                color = Color.LightGray
                            )
                            Text(text = "Moč: ${farm.power} kW", color = Color.LightGray)
                            Text(text = "Status: ${farm.status}", color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun GeneratorScreen() {
    var countText by remember { mutableStateOf("0") }
    var minWindText by remember { mutableStateOf("1") }
    var maxWindText by remember { mutableStateOf("12") }
    var centerLatText by remember { mutableStateOf("") }
    var centerLonText by remember { mutableStateOf("") }
    var radiusText by remember { mutableStateOf("") }
    var selectedArea by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val predefinedCenters = listOf(
        "None" to null,
        "Ljubljana" to Pair(46.1512, 14.9955),
        "Maribor" to Pair(46.5547, 15.6459),
        "Celje" to Pair(46.2389, 15.2672)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Koliko vetrnic želiš ustvariti?", color = Color.White, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = countText,
            onValueChange = { newText ->
                if (newText.all { it.isDigit() }) {
                    countText = newText
                }
            },
            label = { Text("Število vetrnic") },
            singleLine = true,
            modifier = Modifier.width(150.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text("Hitrost vetra (m/s):", color = Color.White, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            OutlinedTextField(
                value = minWindText,
                onValueChange = { newText ->
                    if (newText.all { it.isDigit() }) {
                        minWindText = newText
                    }
                },
                label = { Text("Min") },
                singleLine = true,
                modifier = Modifier.width(75.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedTextField(
                value = maxWindText,
                onValueChange = { newText ->
                    if (newText.all { it.isDigit() }) {
                        maxWindText = newText
                    }
                },
                label = { Text("Max") },
                singleLine = true,
                modifier = Modifier.width(75.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Izberi center lokacije ali vnesi ročno:", color = Color.White, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedTextField(
                value = selectedArea.ifEmpty { "None" },
                onValueChange = {},
                label = { Text("Prednastavljeni centri") },
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                        Modifier.clickable { expanded = true })
                },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                predefinedCenters.forEach { (name, coords) ->
                    DropdownMenuItem(onClick = {
                        selectedArea = name
                        expanded = false
                        if (coords != null) {
                            centerLatText = coords.first.toString()
                            centerLonText = coords.second.toString()
                        } else {
                            centerLatText = ""
                            centerLonText = ""
                        }
                    }) {
                        Text(name)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            OutlinedTextField(
                value = centerLatText,
                onValueChange = { newText ->
                    if (newText.matches(Regex("[-0-9.]*"))) {
                        centerLatText = newText
                        selectedArea = ""
                    }
                },
                label = { Text("Latitude") },
                singleLine = true,
                modifier = Modifier.width(150.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedTextField(
                value = centerLonText,
                onValueChange = { newText ->
                    if (newText.matches(Regex("[-0-9.]*"))) {
                        centerLonText = newText
                        selectedArea = ""
                    }
                },
                label = { Text("Longitude") },
                singleLine = true,
                modifier = Modifier.width(150.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = radiusText,
            onValueChange = { newText ->
                if (newText.matches(Regex("\\d*\\.?\\d*"))) {
                    radiusText = newText
                }
            },
            label = { Text("Radius (km)") },
            singleLine = true,
            modifier = Modifier.width(150.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val count = countText.toIntOrNull() ?: 0
            val minWind = minWindText.toDoubleOrNull() ?: 1.0
            val maxWind = maxWindText.toDoubleOrNull() ?: 12.0
            val radius = radiusText.toDoubleOrNull() ?: 10.0
            println("dela $radius" )

            val defaultLat = 46.1512
            val defaultLon = 14.9955

            val centerLat = centerLatText.toDoubleOrNull() ?: defaultLat
            val centerLon = centerLonText.toDoubleOrNull() ?: defaultLon

            when {
                count <= 0 -> message = "Prosim vnesi veljavno število vetrnic"
                minWind < 0 -> message = "Minimalna hitrost vetra ne more biti negativna"
                maxWind < minWind -> message = "Maksimalna hitrost mora biti večja ali enaka minimalni"
                centerLat !in -90.0..90.0 -> message = "Latitude mora biti med -90 in 90"
                centerLon !in -180.0..180.0 -> message = "Longitude mora biti med -180 in 180"
                radius <= 0 -> message = "Radius mora biti večji od 0"
                else -> {
                    message = ""
                    coroutineScope.launch {
                        try {
                            generateWindmills(count, minWind, maxWind, centerLat, centerLon, radius)
                            message = "Ustvarjenih $count vetrnic"
                        } catch (e: Exception) {
                            message = "Napaka pri generiranju: ${e.message}"
                        }
                    }
                }
            }
        }) {
            Text("Generiraj")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Text(message, color = Color.Green)
        }
    }
}

suspend fun generateWindmills(
    count: Int,
    minWind: Double = 1.0,
    maxWind: Double = 12.0,
    centerLat: Double = 46.1512,
    centerLon: Double = 14.9955,
    radius: Double = 10.0  // radius v km
) {
    val centerLocation = Location.fromLatLon(centerLat, centerLon)
    val stations = mutableListOf<Station>()

    repeat(count) {
        val nearLoc = Location.near(centerLocation, radius)
        val station = Station.randomWind(min = minWind, max = maxWind, location = nearLoc)
        stations.add(station)
    }

    withContext(Dispatchers.IO) {
        Database.windmills.insertMany(stations)
    }

    println("${stations.size} stations saved to DB.")
}


@Composable
fun AboutScreen() {}

@Composable
fun AddUserScreen(){}