package de.hsfl.team46.campusflag

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hsfl.team46.campusflag.model.Player

class PlayersAdapter(private val players: List<Player>, val onClick: (Player) -> Unit) :
    RecyclerView.Adapter<PlayersAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_player, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val name = holder.itemView.findViewById<TextView>(R.id.textViewName)
        name.text = players[position].name.toString()

        val team = holder.itemView.findViewById<TextView>(R.id.textViewTeam)
        team.text = players[position].team.toString()

        val bluetoothStatus = holder.itemView.findViewById<ImageView>(R.id.bluetoothStatus)
        if (players[position].addr == null) {
            bluetoothStatus.setImageResource(android.R.drawable.btn_star_big_off)
        } else {
            bluetoothStatus.setImageResource(android.R.drawable.btn_star_big_on)
        }

        holder.itemView.setOnClickListener {
            onClick(players[position])
        }
    }

    override fun getItemCount() = players.size
}