package com.ireddragonicy.llmdroid.domain.usecase.chat

import com.ireddragonicy.llmdroid.domain.model.ChatSession
import com.ireddragonicy.llmdroid.domain.model.Result
import com.ireddragonicy.llmdroid.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatSessionsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<Result<List<ChatSession>>> {
        return chatRepository.getChatSessions()
    }
}