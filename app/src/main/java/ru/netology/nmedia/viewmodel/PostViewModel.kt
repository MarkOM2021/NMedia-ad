package ru.netology.nmedia.viewmodel

import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.*
import androidx.paging.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRemoteMediator
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.util.SingleLiveEvent
import javax.inject.Inject

private val empty = Post(
    id = 0,
    content = "",
    authorId = 0,
    author = "",
    authorAvatar = "",
    likedByMe = false,
    likes = 0,
    published = 0,
)

private val noPhoto = PhotoModel()

@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    postDao: PostDao,
    apiService: ApiService,
    postRemoteKeyDao: PostRemoteKeyDao,
    appDb: AppDb,
    auth: AppAuth,
) : ViewModel() {

    @OptIn(ExperimentalPagingApi::class)
    private val authInfo: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(10, enablePlaceholders = false),
        remoteMediator = PostRemoteMediator(
            service = apiService,
            appDb = appDb,
            postDao = postDao,
            postRemoteKeyDao = postRemoteKeyDao
        ),
        pagingSourceFactory = { postDao.getPagingSource() },
    ).flow
        .map {
        it.map(PostEntity::toDto)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val data: Flow<PagingData<Post>> = auth.authStateFlow
        .flatMapLatest { (myId, _) ->
            authInfo.map { pagingData ->
                pagingData.map { post ->
                    post.copy(ownedByMe = post.authorId == myId)
                }
            }
        }

    private val _data = MutableLiveData<FeedModel>()

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo

    init {
        loadPosts()
    }

    fun loadPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            //repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun refreshPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(refreshing = true)
            //repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun save() {
        edited.value?.let {
            viewModelScope.launch {
                try {
                    repository.save(
                        it, _photo.value?.uri?.let { MediaUpload(it.toFile()) }
                    )

                    _postCreated.value = Unit
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        edited.value = empty
        _photo.value = noPhoto
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun changePhoto(uri: Uri?) {
        _photo.value = PhotoModel(uri)
    }

    fun likeById(id: Long) {
        lastAction = null
        lastLikedID = null
        viewModelScope.launch {
            try {
                repository.likeById(id)
                _data.postValue(
                    _data.value?.copy(
                        posts = _data.value?.posts.orEmpty()
                            .map {
                                if (it.id == id) it.copy(likedByMe = true, likes = it.likes + 1)
                                else it
                            })
                )
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                lastAction = ActionType.LIKE
                lastLikedID = id
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun disLikeById(id: Long) {
        lastAction = null
        lastDisLikedID = null
        viewModelScope.launch {
            try {
                repository.disLikeById(id)
                _data.postValue(
                    _data.value?.copy(
                        posts = _data.value?.posts.orEmpty()
                            .map {
                                if (it.id == id) it.copy(likedByMe = false, likes = it.likes - 1)
                                else it
                            })
                )
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                lastAction = ActionType.DISLIKE
                lastLikedID = id
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun removeById(id: Long) {
        lastAction = null
        lastRemovedID = null

        viewModelScope.launch {
            try {
                repository.removeById(id)
                val updated = _data.value?.posts.orEmpty().filter {it.id != id}
                _data.postValue(
                    _data.value?.copy(
                        posts = updated
                    )
                )
            } catch (e: Exception) {
                lastAction = ActionType.REMOVE
                lastRemovedID = id
                val old = _data.value?.posts.orEmpty()
                _dataState.value = FeedModelState(error = true)
                _data.postValue(_data.value?.copy(posts = old))
            }
        }
    }

    private var lastAction: ActionType? = null
    private var lastLikedID: Long? = null
    private var lastDisLikedID: Long? = null
    private var lastRemovedID: Long? = null
    private var lastSaved: Long? = null

    fun retry() {
        when (lastAction) {
            ActionType.SAVE -> retrySave()
            ActionType.LIKE -> retryLike()
            ActionType.DISLIKE -> retryDisLike()
            ActionType.REMOVE -> retryRemove()
            else -> loadPosts()
        }
    }

    private fun retryDisLike() {
        lastDisLikedID?.let {
            disLikeById(it)
        }
    }

    private fun retrySave() {
        lastSaved?.let {
            save()
        }
    }

    private fun retryRemove() {
        lastRemovedID?.let {
            removeById(it)
        }
    }

    private fun retryLike() {
        lastLikedID?.let {
            likeById(it)
        }
    }

    enum class ActionType {
        SAVE,
        LIKE,
        REMOVE,
        DISLIKE
    }
}
