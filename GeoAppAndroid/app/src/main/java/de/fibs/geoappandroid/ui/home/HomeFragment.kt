package de.fibs.geoappandroid.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import de.fibs.geoappandroid.R
import de.fibs.geoappandroid.databinding.FragmentHomeBinding
import de.fibs.geoappandroid.service.LocationStepService

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var binding: FragmentHomeBinding

    private val viewModel = HomeViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val serviceIntent = Intent(requireContext(), LocationStepService::class.java)
        requireContext().startForegroundService(serviceIntent)

        checkAndRequestPermissions()

        return binding.root
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (requiredPermissions.isNotEmpty()) {
            requestPermissions(requiredPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            // All permissions are granted; proceed with your functionality
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val perms = permissions.zip(grantResults.toTypedArray()).toMap()
            if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == PackageManager.PERMISSION_GRANTED &&
                perms[Manifest.permission.ACTIVITY_RECOGNITION] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted; proceed with your functionality
            } else {
                // Permissions denied; inform the user that the app cannot function without these permissions
                Toast.makeText(requireContext(), "App does not function without the requested permissions.", Toast.LENGTH_LONG).show()
            }
        }
    }
}