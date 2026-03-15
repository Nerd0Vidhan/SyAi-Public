//package com.mato.syai.mood_tracker
//
//// ... [imports remain unchanged] ...
//
//@Composable
//fun JournalInputBox(
//    journalText: String,
//    onTextChange: (String) -> Unit,
//    onSave: () -> Unit,
//    onCancel: () -> Unit
//) {
//    val wordCount = journalText.length
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(Color(0xFFEFEFEF))
//            .padding(8.dp)
//    ) {
//        OutlinedTextField(
//            value = journalText,
//            onValueChange = {
//                if (it.length <= 200) onTextChange(it)
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(120.dp),
//            maxLines = 6,
//            singleLine = false
//        )
//        Text(
//            text = "$wordCount/200",
//            style = MaterialTheme.typography.bodySmall,
//            modifier = Modifier.align(Alignment.End).padding(end = 8.dp, top = 4.dp)
//        )
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.End
//        ) {
//            TextButton(onClick = onCancel) {
//                Text("Cancel")
//            }
//            Spacer(Modifier.width(8.dp))
//            Button(onClick = onSave) {
//                Text("Save")
//            }
//        }
//    }
//}
