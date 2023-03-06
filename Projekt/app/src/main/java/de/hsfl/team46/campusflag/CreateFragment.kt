package de.hsfl.team46.campusflag

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import de.hsfl.team46.campusflag.custom.CustomMapViewCreate
import de.hsfl.team46.campusflag.databinding.FragmentCreateBinding
import de.hsfl.team46.campusflag.model.Position
import de.hsfl.team46.campusflag.viewmodels.ViewModel


class CreateFragment : Fragment() {

    private val mainViewModel: ViewModel by activityViewModels()

    private var _binding: FragmentCreateBinding? = null
    private val binding get() = _binding!!

    private var customView: CustomMapViewCreate? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Permission", "Granted")
                mainViewModel.getLocationRepository().requestCurrentLocation(binding.root.context)
            } else {
                Toast.makeText(
                    binding.root.context,
                    "GPS Location permission needs to be granted!",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("Permission", "Denied")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentCreateBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = mainViewModel

        customView = binding.karteCampusCreate
        customView!!.viewModel = mainViewModel

        binding.creategameBtnCreate.setOnClickListener {
            mainViewModel.createGame { isError ->
                if (!isError) {
                    findNavController().navigate(R.id.action_create_lobbyy)
                }
            }
        }

        binding.cancelBtn.setOnClickListener {
            mainViewModel.resetSettings()
            findNavController().navigate(R.id.action_create_to_start)
        }

        binding.setflagpositionBtnCreate.setOnClickListener {
            mainViewModel.setCurrentHostFlagPositions(
                Position(
                    mainViewModel.currentLocation.value!!.latitude,
                    mainViewModel.currentLocation.value!!.longitude,
                )
            )
            mainViewModel.setPositions(mainViewModel.currentHostFlagPositions)
            Toast.makeText(
                binding.root.context,
                "Setting the Flag at position.. ",
                Toast.LENGTH_SHORT
            ).show()
        }

        mainViewModel.getPositions().observe(binding.lifecycleOwner!!) {
            if (mainViewModel.getPositions().value != null) {
                val customV: CustomMapViewCreate? = CustomMapViewCreate(binding.root.context, null)
                customV!!.viewModel = mainViewModel

                customV.layoutParams = binding.karteCampusCreate.layoutParams

                binding.frameLayout2.removeView(binding.karteCampusCreate)
                binding.frameLayout2.addView(customV, binding.karteCampusCreate.layoutParams)
            }
        }

        mainViewModel.currentLocation.observe(binding.lifecycleOwner!!) {
            if (mainViewModel.getPositions().value != null) {
                val customV: CustomMapViewCreate? = CustomMapViewCreate(binding.root.context, null)
                customV!!.viewModel = mainViewModel

                customV.layoutParams = binding.karteCampusCreate.layoutParams

                binding.frameLayout2.removeView(binding.karteCampusCreate)
                binding.frameLayout2.addView(customV, binding.karteCampusCreate.layoutParams)
            }
        }

        displayCurrentLocation()

        return binding.root
    }

    private fun displayCurrentLocation() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                binding.root.context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                mainViewModel.getLocationRepository().requestCurrentLocation(binding.root.context)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
}