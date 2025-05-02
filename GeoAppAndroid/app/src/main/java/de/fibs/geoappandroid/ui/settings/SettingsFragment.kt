package de.fibs.geoappandroid.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import de.fibs.geoappandroid.R
import de.fibs.geoappandroid.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)

        viewModel = SettingsViewModel(requireContext())
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }
}