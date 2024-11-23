package com.example.mydialer

import androidx.recyclerview.widget.DiffUtil

class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
    override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        // Сравниваем по идентификатору контакта, в данном случае предполагаем, что уникальным является телефон
        return oldItem.phone == newItem.phone
    }

    override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
        // Сравниваем содержимое элементов контакта
        return oldItem == newItem
    }
}