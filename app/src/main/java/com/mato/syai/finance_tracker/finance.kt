import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mato.syai.finance_tracker.AppModule
import com.mato.syai.finance_tracker.FinanceViewModel

//package com.mato.syai.finance_tracker
//
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
//
//data class Transaction(
//    val id: Int,
//    val title: String,
//    val amount: Double
//)
//
//class FinanceViewModel : ViewModel() {
//
//    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
//    val transactions: StateFlow<List<Transaction>> = _transactions
//
//    val totalBalance: Double
//        get() = _transactions.value.sumOf { it.amount }
//
//    fun addTransaction(title: String, amount: Double) {
//        val newTransaction = Transaction(
//            id = _transactions.value.size + 1,
//            title = title,
//            amount = amount
//        )
//        _transactions.value = _transactions.value + newTransaction
//    }
//}
//
//@Composable
//fun MainScreen(viewModel: FinanceViewModel = viewModel()) {
//    var title by remember { mutableStateOf("") }
//    var amount by remember { mutableStateOf("") }
//
//    val transactions by viewModel.transactions.collectAsState()
//
//    Column(modifier = Modifier.padding(16.dp)) {
//        Text("Finance Tracker", style = MaterialTheme.typography.headlineMedium)
//
//        Spacer(Modifier.height(16.dp))
//
//        Text("Total Balance: ₹${viewModel.totalBalance}", style = MaterialTheme.typography.titleLarge)
//
//        Spacer(Modifier.height(16.dp))
//
//        OutlinedTextField(
//            value = title,
//            onValueChange = { title = it },
//            label = { Text("Title") },
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(Modifier.height(8.dp))
//
//        OutlinedTextField(
//            value = amount,
//            onValueChange = { amount = it },
//            label = { Text("Amount") },
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(Modifier.height(8.dp))
//
//        Button(onClick = {
//            val amt = amount.toDoubleOrNull() ?: 0.0
//            if (title.isNotBlank() && amt != 0.0) {
//                viewModel.addTransaction(title, amt)
//                title = ""
//                amount = ""
//            }
//        }) {
//            Text("Add Transaction")
//        }
//
//        Spacer(Modifier.height(24.dp))
//
//        Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
//        LazyColumn {
//            items(transactions.size) { index ->
//                val txn = transactions[index]
//                TransactionItem(txn)
//            }
//        }
//    }
//}
//
//@Composable
//fun TransactionItem(transaction: Transaction) {
//    Card(modifier = Modifier
//        .fillMaxWidth()
//        .padding(vertical = 4.dp)) {
//        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
//            Text(transaction.title)
//            Text("₹${transaction.amount}")
//        }
//    }
//}
//
//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun FinanceTrackerPreview() {
//    MainScreen()
//}

class FinanceTracker : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppModule.provideDatabase(applicationContext)
        val repository = AppModule.provideRepository(db)
        val viewModel = FinanceViewModel(repository)

        // ✅ Correct usage of Composable inside setContent
        setContent {
            MaterialTheme {
                FinanceTracker(viewModel)
            }
        }
    }
}


@Composable
fun FinanceTracker(viewModel: FinanceViewModel) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Income") }

    val transactions by viewModel.transactions.collectAsState()

    Column(Modifier.padding(16.dp)) {
        Text("Finance Tracker", style = MaterialTheme.typography.headlineSmall)

        Text("Balance: ₹${viewModel.balance}", style = MaterialTheme.typography.titleMedium)
        Text("Income: ₹${viewModel.income} | Expense: ₹${viewModel.expense}")

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

        Row {
            RadioButton(selected = type == "Income", onClick = { type = "Income" })
            Text("Income")
            Spacer(Modifier.width(8.dp))
            RadioButton(selected = type == "Expense", onClick = { type = "Expense" })
            Text("Expense")
        }

        Button(onClick = {
            viewModel.addTransaction(title, amount.toDoubleOrNull() ?: 0.0, type)
            title = ""; amount = ""; type = "Income"
        }) {
            Text("Add Transaction")
        }

        Spacer(Modifier.height(20.dp))
        LazyColumn {
            items(transactions) { txn ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(txn.title)
                            Text(txn.type)
                        }
                        Text("₹${txn.amount}")
                    }
                }
            }
        }
    }
}

@Composable
fun FinanceTrackerScreen() {
    val context = LocalContext.current
    val viewModel = remember {
        val db = AppModule.provideDatabase(context)
        val repo = AppModule.provideRepository(db)
        FinanceViewModel(repo)
    }

    FinanceTracker(viewModel)
}


//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun FinanceTrackerPreview() {
//    val viewModel = FinanceViewModel()
//    FinanceTracker(viewModel)
//}
