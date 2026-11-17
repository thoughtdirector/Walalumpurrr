package com.example.notificacionesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.notificacionesapp.R
import com.example.notificacionesapp.databinding.FragmentInfoLiteBinding

class InfoFragmentLite : BaseFragment<FragmentInfoLiteBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentInfoLiteBinding {
        return FragmentInfoLiteBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        // Todo el contenido está definido en fragment_info_lite.xml.
        // No se requiere poblar texto desde código.
    }

    companion object {
        fun newInstance(): InfoFragmentLite {
            return InfoFragmentLite()
        }
    }
}
