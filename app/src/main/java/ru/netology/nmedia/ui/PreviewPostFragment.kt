package ru.netology.nmedia.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentPostBinding
import ru.netology.nmedia.ui.NewPostFragment.Companion.textArg
import ru.netology.nmedia.util.PostFinder
import ru.netology.nmedia.viewmodel.PostViewModel

class PreviewPostFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPostBinding.inflate(layoutInflater)

        val viewModel: PostViewModel by viewModels(::requireParentFragment)

        val id = arguments?.postID

        viewModel.data.observe(viewLifecycleOwner) { posts ->
            val findPost = posts.posts.find { id == it.id } ?: return@observe
            with(binding){
                postPreview.content.text = findPost.content
                postPreview.published.text = findPost.published.toString()
                postPreview.author.text = findPost.author
                postPreview.like.text = findPost.likes.toString()
                postPreview.share.text = findPost.shares.toString()
                postPreview.menu.setOnClickListener {
                    PopupMenu(binding.root.context, postPreview.menu).apply {
                        inflate(R.menu.options_post)
                        setOnMenuItemClickListener {
                            when (it.itemId) {
                                R.id.remove -> {
                                    findNavController().navigateUp()
                                    true
                                }
                                R.id.edit -> {
                                    findNavController().navigate(R.id.action_post_preview_to_newPostFragment,
                                        Bundle().apply {
                                            textArg = findPost.content
                                        })
                                    true
                                }
                                else -> false
                            }
                        }
                    }
                        .show()
                }

            }
        }

        return binding.root
    }

    companion object {
        var Bundle.postID: Long by PostFinder
    }
}