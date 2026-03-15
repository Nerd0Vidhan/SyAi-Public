package com.mato.syai.finance_tracker

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import androidx.room.Room

@Entity
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "Income" or "Expense"
    val timestamp: Long = System.currentTimeMillis()
)

class FinanceRepository(private val dao: TransactionDao) {
    fun getTransactions() = dao.getAllTransactions()
    suspend fun addTransaction(transaction: Transaction) = dao.insertTransaction(transaction)
}

@Database(entities = [Transaction::class], version = 1)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM `Transaction` ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
}

object AppModule {
    fun provideDatabase(context: Context): FinanceDatabase {
        return Room.databaseBuilder(
            context,
            FinanceDatabase::class.java,
            "finance_db"
        ).build()
    }

    fun provideRepository(db: FinanceDatabase): FinanceRepository {
        return FinanceRepository(db.transactionDao())
    }
}

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {
    private val _transactions = repository.getTransactions().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), emptyList()
    )
    val transactions = _transactions

    val income get() = _transactions.value.filter { it.type == "Income" }.sumOf { it.amount }
    val expense get() = _transactions.value.filter { it.type == "Expense" }.sumOf { it.amount }
    val balance get() = income - expense

    fun addTransaction(title: String, amount: Double, type: String) {
        viewModelScope.launch {
            val txn = Transaction(title = title, amount = amount, type = type)
            repository.addTransaction(txn)
        }
    }
}
