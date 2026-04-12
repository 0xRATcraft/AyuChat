package ru.fromchat.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

import ru.fromchat.ui.M3SearchBar

@Composable
fun DebugSearchBarDocsScreen() {
    SearchBarExamples()
}

@Preview(showBackground = true)
@Composable
fun SearchBarExamples() {
    val allItems = remember {
        listOf(
            "Cupcake",
            "Donut",
            "Eclair",
            "Froyo",
            "Honeycomb",
            "Ice Cream Sandwich",
            "Jelly Bean",
            "KitKat",
            "Lollipop",
            "Marshmallow",
            "Nougat",
            "Oreo",
            "Pie"
        )
    }

    var query by remember { mutableStateOf("") }
    val filtered by remember {
        derivedStateOf {
            val searchText = query.trim().lowercase()
            if (searchText.isEmpty()) {
                allItems
            } else {
                allItems.filter { it.lowercase().contains(searchText) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        M3SearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = {},
            placeholder = "Search",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Results")

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filtered) { item ->
                Text(
                    text = item,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}
