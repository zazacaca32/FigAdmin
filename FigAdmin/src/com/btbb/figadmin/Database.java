/*
Copyright (C) 2011, 2016 Serge Humphrey <sergehumphrey@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package com.btbb.figadmin;

import java.util.ArrayList;
import java.util.UUID;

public abstract class Database {

	protected FigAdmin plugin;

	public abstract boolean initialize(FigAdmin plugin);

	/*
	 * Remove a player from banlist
	 */
	public abstract boolean removeFromBanlist(UUID playerId);
	public abstract boolean removeFromBanlist(String playerName);
	
	public abstract void addPlayer(EditBan e);
	

	/*
	 * Get Banned players
	 * Player,Reason
	 */
	public abstract ArrayList<EditBan> getBannedPlayers();
	
	protected abstract EditBan loadFullRecord(UUID playerId);

    protected abstract EditBan loadFullRecord(int id);
    
    protected abstract boolean deleteFullRecord(int id);
    
    public abstract int unbanIP(String ip);
    public abstract ArrayList<EditBan> listRecords(UUID uuid);
    
    public abstract ArrayList<EditBan> listRecords(String name, boolean exact);
    
    public abstract boolean saveFullRecord(EditBan ban);
    
    public abstract int getWarnCount(UUID playerId);
    
    /**
     * Clears warnings from player
     * 
     * @param playerId player's uuid
     * 
     * @return How many warnings were deleted
     */
    public abstract int clearWarnings(UUID playerId);
	
}