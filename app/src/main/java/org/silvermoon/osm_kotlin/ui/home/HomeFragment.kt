package org.silvermoon.osm_kotlin.ui.home

import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import org.silvermoon.osm_kotlin.R
import org.silvermoon.osm_kotlin.listeners.IMapInteractionListener
import org.silvermoon.osm_kotlin.location.LocationListenerHelper
import org.silvermoon.osm_kotlin.mapunits.MapMarker
import org.silvermoon.osm_kotlin.mapunits.OsmLocationOverlay
import org.silvermoon.osm_kotlin.mapunits.OsmMapView
import org.silvermoon.osm_kotlin.mapunits.OsmMapView.OsmMapViewBuilder
import org.silvermoon.osm_kotlin.model.OSMModel
import org.silvermoon.osm_kotlin.model.OsmDatabaseHelper
import java.io.File


class HomeFragment : Fragment(), IMapInteractionListener,
    LocationListenerHelper.IMyLocationListener {

    private var mLocationListener: LocationListenerHelper? = null
    private var mOsmMapView: OsmMapView? = null
    private var mOsmLocationOverlay: OsmLocationOverlay? = null
    private var mapLayout: ViewGroup? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_home, container, false)
        // val textView: TextView = root.findViewById(R.id.text_home)
        mapLayout = root.findViewById(R.id.mapLayout) as ViewGroup

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLocationListener = LocationListenerHelper(requireContext());

        initOsmDatabase();

        initMap();
    }

    override fun onResume() {
        super.onResume()
        mLocationListener!!.startListeningLocation(this)
    }

    override fun onPause() {
        super.onPause()
        mLocationListener!!.stopListeningLocation()
    }

    override fun onDestroy() {
        mOsmMapView!!.clear()
        super.onDestroy()
    }

    private fun initMap() {
        val mapBuilder = OsmMapViewBuilder()
        mapBuilder.isNetworkRequestAllowed = true
        mapBuilder.positionIndicatorDrawableId = R.drawable.position_indicator
        mOsmMapView = OsmMapView(requireActivity().applicationContext, mapBuilder, this)
        mOsmLocationOverlay = OsmLocationOverlay(
            requireActivity().applicationContext, mapBuilder,
            mOsmMapView!!
        )
        mOsmMapView!!.addOverlay(mOsmLocationOverlay!!)

        val layoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.FILL_PARENT
        )
        mapLayout!!.addView(mOsmMapView, layoutParams)
        mOsmMapView!!.setCenter(37.7793, -122.4192)
        mOsmMapView!!.setZoom(12)
    }

    private fun initOsmDatabase() {
        val destFile = File(requireActivity().filesDir, "osm_db.sqlite")
        val osmDbHelper = OsmDatabaseHelper(requireContext())
        osmDbHelper.databaseFile = destFile
        val success = osmDbHelper.openOrCreateDatabase(requireContext(), destFile)
        if (success) {
            OSMModel.mDbHelper = osmDbHelper
        }
    }

    override fun onMapTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    override fun onMapDraw(canvas: Canvas?) {}

    override fun onMapSingleTapConfirmed(event: MotionEvent?) {}

    override fun onMapStopPanning() {}

    override fun onMapZoomChanged(zoomLevel: Int) {}

    override fun onMapLongClick(event: MotionEvent?) {}

    override fun onMapMarkerTap(overlayItem: MapMarker?) {}

    override fun onMapCalloutTap(event: MotionEvent?) {}

    override fun onNewLocation(location: Location?) {
        if (mOsmLocationOverlay != null && location != null) {
            mOsmLocationOverlay!!.setLocation(location)
        }
    }
}