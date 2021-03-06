package de.uulm.automotiveuulmapp.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.uulm.automotiveuulmapp.R

/**
 * A simple [Fragment] subclass as the fifth Welcome App Introduction slider content.
 * Contains an explanation on how to subscribe for topics.
 */
class IntroFragmentMessageHistoryExplanation : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_intro_message_history_explanation, container, false)
    }
}
