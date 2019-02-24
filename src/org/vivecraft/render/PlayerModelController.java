package org.vivecraft.render;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.vivecraft.api.VRData;
import org.vivecraft.utils.Quaternion;
import org.vivecraft.utils.Utils;
import org.vivecraft.utils.Vector3;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Particles;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Created by Hendrik on 07-Aug-16.
 */
public class PlayerModelController {
	private final Minecraft mc;
	private Map<UUID, RotInfo> vivePlayers = new HashMap<UUID, RotInfo>();
	private Map<UUID, RotInfo> vivePlayersLast = new HashMap<UUID, RotInfo>();
	private Map<UUID, RotInfo> vivePlayersReceived = Collections.synchronizedMap(new HashMap<UUID, RotInfo>());
	

	static PlayerModelController instance;
	public static PlayerModelController getInstance(){
		if(instance==null)
			instance=new PlayerModelController();
		return instance;
	}
	
	private PlayerModelController() {
		this.mc = Minecraft.getMinecraft();
	}

	public static class RotInfo{ 
		public RotInfo(){
			
		}
		public boolean seated, reverse;
		public int hmd = 0;
		public Quaternion leftArmQuat, rightArmQuat, headQuat; 
		public Vec3d leftArmRot, rightArmRot, headRot; 
		public Vec3d leftArmPos, rightArmPos, Headpos;
	}
	
	private Random rand = new Random();

	public void Update(UUID uuid, byte[] hmddata, byte[] c0data, byte[] c1data, boolean localPlayer) {
		if (!localPlayer && mc.player.getUniqueID().equals(uuid))
			return; // Don't update local player from server packet
	
		Vec3d hmdpos = null, c0pos = null, c1pos = null;
		Quaternion hmdq = null, c0q = null, c1q = null;
		boolean seated=false, reverse = false;
		for (int i = 0; i <= 2; i++) {
			try {
				byte[]arr = null;
				switch(i){
				case 0:	arr = hmddata;
				break;
				case 1: arr = c0data;
				break;
				case 2: arr = c1data;
				break;
				}

				ByteArrayInputStream by = new ByteArrayInputStream(arr);
				DataInputStream da = new DataInputStream(by);

				boolean bool = false;
				if(arr.length >=29)
					bool = da.readBoolean();		

				float posx = da.readFloat();
				float posy = da.readFloat();
				float posz = da.readFloat();
				float rotw = da.readFloat();
				float rotx = da.readFloat();
				float roty = da.readFloat();
				float rotz = da.readFloat();

				da.close();
				
				switch(i){
				case 0:	
					if(bool){ //seated
						seated = true;
					}
					hmdpos = new Vec3d(posx, posy, posz);
					hmdq = new Quaternion(rotw, rotx, roty, rotz);
					break;
				case 1: 
					if(bool){ //reverse
						reverse = true;
					}
					c0pos = new Vec3d(posx, posy, posz);
					c0q = new Quaternion(rotw, rotx, roty, rotz);
					break;
				case 2: 
					if(bool){ //reverse
						reverse = true;
					}
					c1pos = new Vec3d(posx, posy, posz);
					c1q = new Quaternion(rotw, rotx, roty, rotz);
					break;
				}
				
			} catch (IOException e) {

			}
		}
	
		Vector3 shoulderR=new Vector3(0,-0.0f,0);
		Vector3 forward = new Vector3(0,0,-1);
		Vector3 dir = hmdq.multiply(forward);
		Vector3 dir2 = c0q.multiply(forward);
		Vector3 dir3 = c1q.multiply(forward);
		
		RotInfo out = new RotInfo();
		out.reverse =reverse;
		out.seated = seated;
		if(donors.containsKey(uuid))out.hmd = donors.get(uuid);
		out.leftArmRot=new Vec3d(dir3.getX(), dir3.getY(), dir3.getZ());
		out.rightArmRot=new Vec3d(dir2.getX(), dir2.getY(), dir2.getZ());
		out.headRot = new Vec3d(dir.getX(), dir.getY(), dir.getZ());
		out.Headpos = hmdpos;
		out.leftArmPos = c1pos;
		out.rightArmPos = c0pos;
		out.leftArmQuat = c1q;
		out.rightArmQuat = c0q;
		out.headQuat = hmdq;	
	
		vivePlayersReceived.put(uuid, out);

	}
	
	public void Update(UUID uuid, byte[] hmddata, byte[] c0data, byte[] c1data) {
		Update(uuid, hmddata, c0data, c1data, false);
	}
	
	public void tick() {
		for (Map.Entry<UUID, RotInfo> entry : vivePlayers.entrySet()) {
			vivePlayersLast.put(entry.getKey(), entry.getValue());
		}
		for (Map.Entry<UUID, RotInfo> entry : vivePlayersReceived.entrySet()) {
			vivePlayers.put(entry.getKey(), entry.getValue());
		}
		for (Iterator<UUID> it = vivePlayers.keySet().iterator(); it.hasNext();) {
			UUID uuid = it.next();
			World world = Minecraft.getMinecraft().world;
			if (world != null) {
				if (world.getPlayerEntityByUUID(uuid) == null) {
					it.remove();
					vivePlayersLast.remove(uuid);
					vivePlayersReceived.remove(uuid);
				} else {
					RotInfo out = vivePlayers.get(uuid);
					if(out.hmd > 3 && rand.nextInt(10) < 4){
						Vec3d derp = out.headRot.scale(0.1f);
						Particle particle = mc.effectRenderer.addParticle(Particles.FIREWORK,
								out.Headpos.x+ ((double)this.rand.nextFloat() - 0.5D)*.02f,
								out.Headpos.y - 0.8f + ((double)this.rand.nextFloat() - 0.5D)*.02f,
								out.Headpos.z + ((double)this.rand.nextFloat()- 0.5D)*.02f,
								-derp.x + ((double)this.rand.nextFloat()- 0.5D)*.01f,((double)this.rand.nextFloat()- .05f)*.05f, -derp.z + ((double)this.rand.nextFloat()- 0.5D)*.01f
						);
						if (particle != null)
							particle.setColor(0.5F + rand.nextFloat() / 2, 0.5F + rand.nextFloat() / 2, 0.5F + rand.nextFloat() / 2);
					}
				}
			}
		}
	}
	
	private Map<UUID, Integer> donors = new HashMap<UUID, Integer>();

	public void setHMD(UUID uuid, int level){
		donors.put(uuid, level);
	}
	
	public boolean HMDCHecked(UUID uuid){
		return donors.containsKey(uuid);
	}
	
	public RotInfo getRotationsForPlayer(UUID uuid){
		if (debug) uuid = mc.player.getUniqueID();
		RotInfo rot = vivePlayers.get(uuid);
		if (rot != null && vivePlayersLast.containsKey(uuid)) {
			RotInfo rotLast = vivePlayersLast.get(uuid);
			RotInfo rotLerp = new RotInfo();
			float pt = Minecraft.getMinecraft().getRenderPartialTicks();
			rotLerp.reverse = rot.reverse;
			rotLerp.seated = rot.seated;
			rotLerp.hmd = rot.hmd;
			rotLerp.leftArmPos = Utils.vecLerp(rotLast.leftArmPos, rot.leftArmPos, pt);
			rotLerp.rightArmPos = Utils.vecLerp(rotLast.rightArmPos, rot.rightArmPos, pt);
			rotLerp.Headpos = Utils.vecLerp(rotLast.Headpos, rot.Headpos, pt);
			rotLerp.leftArmQuat = rot.leftArmQuat;//Utils.slerp(rotLast.leftArmQuat, rot.leftArmQuat, pt);
			rotLerp.rightArmQuat =rot.rightArmQuat;//Utils.slerp(rotLast.rightArmQuat, rot.rightArmQuat, pt);
			rotLerp.headQuat = rot.headQuat;//Utils.slerp(rotLast.headQuat, rot.headQuat, pt);
			Vector3 forward = new Vector3(0,0,-1);
			rotLerp.leftArmRot = Utils.vecLerp(rotLast.leftArmRot,Utils.convertToVec3d(rotLerp.leftArmQuat.multiply(forward)), pt);
			rotLerp.rightArmRot = Utils.vecLerp(rotLast.rightArmRot, Utils.convertToVec3d(rotLerp.rightArmQuat.multiply(forward)), pt);
			rotLerp.headRot = Utils.vecLerp(rotLast.headRot,Utils.convertToVec3d(rotLerp.headQuat.multiply(forward)), pt);
			return rotLerp;
		}
		return rot;
	}

	/**
	 * gets the {@link RotInfo} for both SinglePlayer and Multiplayer {@link EntityPlayer}s
	 * */
	public RotInfo getRotationFromEntity(EntityPlayer player){
		UUID playerId = player.getGameProfile().getId();
		if (mc.player.getUniqueID().equals(playerId)) {
			VRData data=Minecraft.getMinecraft().vrPlayer.vrdata_world_render;
			RotInfo rotInfo=new RotInfo();

			Quaternion quatLeft=new Quaternion(data.getController(1).getMatrix());
			Quaternion quatRight=new Quaternion(data.getController(0).getMatrix());
			Quaternion quatHmd=new Quaternion(data.hmd.getMatrix());

			rotInfo.headQuat=quatHmd;
			rotInfo.leftArmQuat=quatLeft;
			rotInfo.rightArmQuat=quatRight;
			rotInfo.seated=mc.vrSettings.seated;

			rotInfo.leftArmPos = data.getController(1).getPosition();
			rotInfo.rightArmPos = data.getController(0).getPosition();
			rotInfo.Headpos = data.hmd.getPosition(); 

			return rotInfo;

		} else {
			return PlayerModelController.getInstance().getRotationsForPlayer(playerId);
		}
	}

	public boolean debug = false;

	public boolean isTracked(UUID uuid){
		debug = false;
		if(debug) return true;
		return vivePlayers.containsKey(uuid);
	}
}
