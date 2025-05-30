package com.example.notificacionesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("Fragment $this binding cannot be accessed before onCreateView() or after onDestroyView()")

    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            _binding = getViewBinding(inflater, container)
            return binding.root
        } catch (e: Exception) {
            android.util.Log.e("BaseFragment", "Error creating view binding: ${e.message}")
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            setupUI()
        } catch (e: Exception) {
            android.util.Log.e("BaseFragment", "Error in setupUI: ${e.message}")
        }
    }

    abstract fun setupUI()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}