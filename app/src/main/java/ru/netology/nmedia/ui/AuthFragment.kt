package ru.netology.nmedia.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.databinding.FragmentAuthBinding
import ru.netology.nmedia.viewmodel.SetAuthViewModel

@AndroidEntryPoint
class AuthFragment : Fragment() {

    private val viewModel: SetAuthViewModel by viewModels(
        ownerProducer = ::requireParentFragment,
    )

    private var fragmentBinding: FragmentAuthBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAuthBinding.inflate(
            inflater,
            container,
            false
        )
        fragmentBinding = binding

        binding.auth.setOnClickListener {
            viewModel.updateUser(
                binding.login.editText?.text.toString(),
                binding.password.editText?.text.toString()
            )
            findNavController().navigateUp()
        }

        return binding.root
    }
}