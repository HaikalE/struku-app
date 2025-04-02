package com.example.struku.di

import com.example.struku.domain.repository.BudgetRepository
import com.example.struku.domain.repository.CategoryRepository
import com.example.struku.domain.repository.OcrRepository
import com.example.struku.domain.repository.ReceiptRepository
import com.example.struku.domain.usecase.CategorizeExpenseUseCase
import com.example.struku.domain.usecase.GetMonthlyReportUseCase
import com.example.struku.domain.usecase.ScanReceiptUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideScanReceiptUseCase(
        ocrRepository: OcrRepository,
        receiptRepository: ReceiptRepository
    ): ScanReceiptUseCase {
        return ScanReceiptUseCase(ocrRepository, receiptRepository)
    }

    @Provides
    @Singleton
    fun provideGetMonthlyReportUseCase(
        receiptRepository: ReceiptRepository,
        budgetRepository: BudgetRepository
    ): GetMonthlyReportUseCase {
        return GetMonthlyReportUseCase(receiptRepository, budgetRepository)
    }

    @Provides
    @Singleton
    fun provideCategorizeExpenseUseCase(
        categoryRepository: CategoryRepository
    ): CategorizeExpenseUseCase {
        return CategorizeExpenseUseCase(categoryRepository)
    }
}
