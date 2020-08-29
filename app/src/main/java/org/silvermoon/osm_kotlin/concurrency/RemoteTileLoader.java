package org.silvermoon.osm_kotlin.concurrency;

import android.os.Handler;
import android.os.Message;

import org.silvermoon.osm_kotlin.mapunits.Tile;
import org.silvermoon.osm_kotlin.model.entities.MapTile;

public class RemoteTileLoader extends Thread {

    private RequestsQueue mRequestsQueue;
    private Handler mHandler;
    private RequestTile mRequestTile;


    public RemoteTileLoader(Handler handler, int tileStackSizeLimit) {
        mHandler = handler;
        mRequestsQueue = new RequestsQueue(1, tileStackSizeLimit);
        mRequestTile = new RequestTile();
        start();
    }

    public void queueTileRequest(Tile tile) {
        //Log.i("AddedToQueue", "x= " +tile.mapX+ " y=" +tile.mapY+ " zoom=" + tile.zoom + " mapTypeId=" + tile.mapTypeId + " ADDED TO QUEUE!");
        mRequestsQueue.queue(tile);
        //Log.i("AddedToQueue", "ADDED TOQUEUE size= " + mRequestsQueue.size() + " time=" + (Calendar.getInstance().getTimeInMillis() - time) + "ms");
        synchronized (this) {
            this.notify();
        }
    }

    private boolean loadTile(Tile tile) {
        if (tile == null || tile.getKey() == null) {
            return false;
        }
        try {
            byte[] bitmapData = mRequestTile.loadBitmap(tile);
            if (bitmapData == null || bitmapData.length == 0)
                return false;

            addTile(tile, bitmapData);
            return true;
        } catch (Exception e) {

        }
        return false;
    }


    public void addTile(Tile tile, final byte[] bitmapData) {
        if (tile == null || bitmapData == null || bitmapData.length == 0) {
            return;
        }
        MapTile.Companion.insertTile(tile, bitmapData);
    }

    @Override
    public void run() {
        Tile tile;

        while (true) {
            tile = null;
            if (mRequestsQueue.hasRequest()) {
                tile = mRequestsQueue.dequeue();
            }
            if (tile != null) {
                boolean loadTileSuccess = loadTile(tile);

                Message message = mHandler.obtainMessage();
                message.arg1 = mRequestsQueue.size();
                message.arg2 = mRequestsQueue.getId();

                if (loadTileSuccess)
                    message.what = TileHandler.TILE_LOADED;
                else
                    message.what = TileHandler.TILE_NOT_LOADED;
                mHandler.sendMessage(message);
            }
            try {
                synchronized (this) {
                    if (mRequestsQueue.size() == 0) {
                        this.wait();
                    }
                }
                Thread.sleep(150);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
