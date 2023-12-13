package com.udacity.project4.locationreminders.reminderslist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.FirebaseUserLiveData
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import com.udacity.project4.utils.setup
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderListFragment : BaseFragment() {

    // Use Koin to retrieve the ViewModel instance
    override val _viewModel: RemindersListViewModel by viewModel()
    private lateinit var binding: FragmentRemindersBinding

    private var firebaseUserLiveData: FirebaseUserLiveData = FirebaseUserLiveData()

    private val requestLocationPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                _viewModel.setUserAvailableToSaveReminders()
            } else {
                setupSelectionObserver()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_reminders,
            container,
            false,
        )
        binding.viewModel = _viewModel

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(false)
        setTitle(getString(R.string.app_name))
        binding.refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        setupFirebaseUserObserver()
        setupRecyclerView()
        binding.addReminderFAB.setOnClickListener {
            navigateToAddReminder()
        }

        _viewModel.isAvailableToSaveAReminder.observe(viewLifecycleOwner) { isUserAvailableToSaveReminders ->
            binding.addReminderFAB.isEnabled = isUserAvailableToSaveReminders
        }

        _viewModel.showLoading.observe(viewLifecycleOwner) { showLoading ->
            binding.refreshLayout.isRefreshing = showLoading
        }

        observeAuthenticationState()
    }

    private fun setupSelectionObserver() {
        _viewModel.currentLocationPermission.observe(viewLifecycleOwner) { currentPermission ->
            when (currentPermission) {
                CurrentLocationPermission.NOT_GRANTED -> {
                    showPermissionDeniedMessage(getString(R.string.permission_denied_explanation))
                }

                CurrentLocationPermission.COARSE -> {
                    showPermissionDeniedMessage(getString(R.string.precise_permission_denied_explanation))
                }

                else -> {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Load the reminders list on the ui
        _viewModel.loadReminders()
    }

    private fun navigateToAddReminder() {
        // Use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(ReminderListFragmentDirections.toSaveReminder()),
        )
    }

    private fun setupRecyclerView() {
        val adapter = RemindersListAdapter { reminder ->
            _viewModel.navigationCommand.postValue(
                NavigationCommand.To(ReminderListFragmentDirections.toSaveReminder()),
            )
        }
        // Setup the recycler view using the extension function
        binding.reminderssRecyclerView.setup(adapter)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                AuthUI.getInstance().signOut(requireContext())
                findNavController().popBackStack()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // Display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
    }

    private fun launchAuthenticationActivity() {
        val intent = Intent(requireContext(), AuthenticationActivity::class.java)
        startActivity(intent)
    }

    private fun isPermissionGranted(): Boolean {
        return when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            -> {
                _viewModel.setCurrentLocationPermission(CurrentLocationPermission.PRECISE)
                true
            }

            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            -> {
                _viewModel.setCurrentLocationPermission(CurrentLocationPermission.COARSE)
                false
            }

            else -> {
                _viewModel.setCurrentLocationPermission(CurrentLocationPermission.NOT_GRANTED)
                false
            }
        }
    }

    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            _viewModel.setUserAvailableToSaveReminders()
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun showPermissionDeniedMessage(message: String) {
        val snackBar = Snackbar.make(
            requireActivity().findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_INDEFINITE,
        )
        snackBar.setAction(getString(R.string.enable_location_button_text)) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            intent.data = uri
            startActivity(intent)
        }
        snackBar.show()
    }

    private fun observeAuthenticationState() {
        _viewModel.authenticationState.observe(viewLifecycleOwner) { authenticationState ->
            when (authenticationState) {
                RemindersListViewModel.AuthenticationState.AUTHENTICATED -> {
                    binding.addReminderFAB.isEnabled = true
                    enableMyLocation()
                }

                else -> {
                    binding.addReminderFAB.isEnabled = false
                    launchAuthenticationActivity()
                }
            }
        }
    }

    private fun setupFirebaseUserObserver() {
        firebaseUserLiveData.observeForever { user ->
            _viewModel.setAuthenticationState(
                if (user != null) {
                    RemindersListViewModel.AuthenticationState.AUTHENTICATED
                } else {
                    RemindersListViewModel.AuthenticationState.UNAUTHENTICATED
                },
            )
        }
    }
}
