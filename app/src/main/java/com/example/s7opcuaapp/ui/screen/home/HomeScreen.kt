package com.example.s7opcuaapp.ui.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.example.s7opcuaapp.R

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to OPC UA App", modifier = Modifier.padding(bottom = 16.dp))
        Image(
            painter = painterResource(id = R.drawable.ic_home_image),
            contentDescription = "Home Image",
            modifier = Modifier.size(150.dp)
        )
    }
}
