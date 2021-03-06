/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2016 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.content;

import android.util.Log;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.actions.ParallelAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.common.BroadcastSequenceMap;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.FileChecksumContainer;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.NfcTagData;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.content.actions.ExtendedActions;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.PlaySoundBrick;
import org.catrobat.catroid.content.bricks.SetLookBrick;
import org.catrobat.catroid.content.bricks.UserBrick;
import org.catrobat.catroid.content.bricks.UserScriptDefinitionBrick;
import org.catrobat.catroid.content.bricks.WhenNfcBrick;
import org.catrobat.catroid.formulaeditor.DataContainer;
import org.catrobat.catroid.formulaeditor.UserVariable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Sprite implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;
	private static final String TAG = Sprite.class.getSimpleName();

	public transient Look look = new Look(this);
	public transient boolean isPaused;

	@XStreamAsAttribute
	private String name;
	private List<Script> scriptList = new ArrayList<>();
	private List<LookData> lookList = new ArrayList<>();
	private List<SoundInfo> soundList = new ArrayList<>();
	private List<UserBrick> userBricks = new ArrayList<>();
	private List<NfcTagData> nfcTagList = new ArrayList<>();
	private transient int newUserBrickNext = 1;
	public transient boolean isBackpackSprite = false;
	public transient boolean isBackgroundSprite = false;

	public Sprite(String name) {
		this.name = name;
	}

	public Sprite() {
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Sprite)) {
			return false;
		}
		if (obj == this) {
			return true;
		}

		Sprite sprite = (Sprite) obj;
		if (sprite.name.equals(this.name)) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() * TAG.hashCode();
	}

	private Object readResolve() {
		//filling FileChecksumContainer:
		if (ProjectManager.getInstance().getCurrentProject() != null) {
			FileChecksumContainer container = ProjectManager.getInstance().getFileChecksumContainer();
			for (SoundInfo soundInfo : soundList) {
				container.addChecksum(soundInfo.getChecksum(), soundInfo.getAbsolutePath());
			}
			for (LookData lookData : lookList) {
				container.addChecksum(lookData.getChecksum(), lookData.getAbsolutePath());
			}
		}
		return this;
	}

	public List<Script> getScriptList() {
		return scriptList;
	}

	public List<Brick> getListWithAllBricks() {
		List<Brick> allBricks = new ArrayList<>();
		for (Script script : scriptList) {
			allBricks.add(script.getScriptBrick());
			allBricks.addAll(script.getBrickList());
		}
		return allBricks;
	}

	public List<Brick> getAllBricks() {
		List<Brick> result = new ArrayList<>();
		for (Script script : scriptList) {
			for (Brick brick : script.getBrickList()) {
				result.add(brick);
			}
		}
		return result;
	}

	public List<SetLookBrick> getSetLookBricks() {
		List<SetLookBrick> result = new ArrayList<>();
		for (Brick brick : getAllBricks()) {
			if (brick instanceof SetLookBrick) {
				result.add((SetLookBrick) brick);
			}
		}
		return result;
	}

	public List<PlaySoundBrick> getPlaySoundBricks() {
		List<PlaySoundBrick> result = new ArrayList<>();
		for (Brick brick : getAllBricks()) {
			if (brick instanceof PlaySoundBrick) {
				result.add((PlaySoundBrick) brick);
			}
		}
		return result;
	}

	public List<WhenNfcBrick> getNfcBrickList() {
		List<WhenNfcBrick> result = new ArrayList<>();
		for (Brick brick : getAllBricks()) {
			if (brick instanceof WhenNfcBrick) {
				result.add((WhenNfcBrick) brick);
			}
		}
		return result;
	}

	public void resetSprite() {
		look = new Look(this);
		for (LookData lookData : lookList) {
			lookData.resetLookData();
		}
	}

	public void removeUserBrick(UserBrick brickToRemove) {
		for (UserBrick userBrick : userBricks) {
			userBrick.getDefinitionBrick().getUserScript().removeInstancesOfUserBrick(brickToRemove);
		}

		for (Script script : scriptList) {
			script.removeInstancesOfUserBrick(brickToRemove);
		}

		userBricks.remove(brickToRemove);
	}

	public UserBrick addUserBrick(UserBrick brick) {
		if (userBricks == null) {
			userBricks = new ArrayList<>();
		}
		userBricks.add(brick);
		return brick;
	}

	public List<UserBrick> getUserBrickList() {
		if (userBricks == null) {
			userBricks = new ArrayList<>();
		}
		return userBricks;
	}

	public void createStartScriptActionSequenceAndPutToMap(Map<String, List<String>> scriptActions) {
		for (int scriptCounter = 0; scriptCounter < scriptList.size(); scriptCounter++) {
			Script script = scriptList.get(scriptCounter);
			if (script instanceof StartScript) {
				Action sequenceAction = createActionSequence(script);
				look.addAction(sequenceAction);
				BroadcastHandler.getActionScriptMap().put(sequenceAction, script);
				BroadcastHandler.getScriptSpriteMapMap().put(script, this);
				String actionName = sequenceAction.toString() + Constants.ACTION_SPRITE_SEPARATOR + name + scriptCounter;
				if (scriptActions.containsKey(Constants.START_SCRIPT)) {
					scriptActions.get(Constants.START_SCRIPT).add(actionName);
					BroadcastHandler.getStringActionMap().put(actionName, sequenceAction);
				} else {
					List<String> startScriptList = new ArrayList<String>();
					startScriptList.add(actionName);
					scriptActions.put(Constants.START_SCRIPT, startScriptList);
					BroadcastHandler.getStringActionMap().put(actionName, sequenceAction);
				}
			}

			if (script instanceof BroadcastScript) {
				BroadcastScript broadcastScript = (BroadcastScript) script;
				SequenceAction action = createActionSequence(broadcastScript);
				BroadcastHandler.getActionScriptMap().put(action, script);
				BroadcastHandler.getScriptSpriteMapMap().put(script, this);
				putBroadcastSequenceAction(broadcastScript.getBroadcastMessage(), action);
				String actionName = action.toString() + Constants.ACTION_SPRITE_SEPARATOR + name + scriptCounter;

				if (scriptActions.containsKey(Constants.BROADCAST_SCRIPT)) {
					scriptActions.get(Constants.BROADCAST_SCRIPT).add(actionName);
					BroadcastHandler.getStringActionMap().put(actionName, action);
				} else {
					List<String> broadcastScriptList = new ArrayList<String>();
					broadcastScriptList.add(actionName);
					scriptActions.put(Constants.BROADCAST_SCRIPT, broadcastScriptList);
					BroadcastHandler.getStringActionMap().put(actionName, action);
				}
			}
		}
	}

	private void putBroadcastSequenceAction(String broadcastMessage, SequenceAction action) {
		if (BroadcastSequenceMap.containsKey(broadcastMessage)) {
			BroadcastSequenceMap.get(broadcastMessage).add(action);
		} else {
			ArrayList<SequenceAction> actionList = new ArrayList<SequenceAction>();
			actionList.add(action);
			BroadcastSequenceMap.put(broadcastMessage, actionList);
		}
	}

	@Override
	public Sprite clone() {
		final Sprite cloneSprite = new Sprite();
		cloneSprite.setName(this.getName());
		cloneSprite.isBackpackSprite = false;

		Project currentProject = ProjectManager.getInstance().getCurrentProject();
		if (currentProject == null || !currentProject.getSpriteList().contains(this)) {
			throw new RuntimeException("The sprite must be in the current project before cloning it.");
		}
		DataContainer userVariables = currentProject.getDataContainer();
		List<UserVariable> originalSpriteVariables = userVariables.getOrCreateVariableListForSprite(this);
		List<UserVariable> clonedSpriteVariables = userVariables.getOrCreateVariableListForSprite(cloneSprite);
		for (UserVariable variable : originalSpriteVariables) {
			clonedSpriteVariables.add(new UserVariable(variable.getName(), variable.getValue()));
		}

		List<LookData> cloneLookList = new ArrayList<>();
		for (LookData element : this.lookList) {
			cloneLookList.add(element.clone());
		}
		cloneSprite.lookList = cloneLookList;

		List<SoundInfo> cloneSoundList = new ArrayList<>();
		for (SoundInfo element : this.soundList) {
			cloneSoundList.add(element.copySoundInfoForSprite(cloneSprite));
		}
		cloneSprite.soundList = cloneSoundList;
		List<UserBrick> cloneUserBrickList = new ArrayList<>();

		for (UserBrick original : userBricks) {
			int originalId = original.getUserBrickId();
			UserBrick deepClone = new UserBrick(originalId);
			deepClone.setUserScriptDefinitionBrickElements(original.getUserScriptDefinitionBrickElements().clone());
			deepClone.updateUserBrickParameters(original.getUserBrickParameters());
			cloneUserBrickList.add(deepClone);
		}

		// once all the UserBricks have been copied over, we can copy their scripts over as well
		// (preserve recursive references)
		for (Brick cloneBrick : cloneUserBrickList) {
			UserBrick deepClone = (UserBrick) cloneBrick;
			UserBrick original = findBrickWithId(userBricks, deepClone.getUserBrickId());

			Script originalScript = original.getDefinitionBrick().getUserScript();
			Script newScript = originalScript.copyScriptForSprite(cloneSprite, cloneUserBrickList);
			newScript.setBrick(deepClone.getDefinitionBrick());
			deepClone.getDefinitionBrick().setUserScript((StartScript) newScript);
		}

		List<NfcTagData> cloneNfcTagList = new ArrayList<NfcTagData>();
		for (NfcTagData element : this.nfcTagList) {
			cloneNfcTagList.add(element.clone());
		}
		cloneSprite.nfcTagList = cloneNfcTagList;

		//The scripts have to be the last copied items
		List<Script> cloneScriptList = new ArrayList<Script>();
		for (Script element : this.scriptList) {
			Script addElement = element.copyScriptForSprite(cloneSprite, cloneUserBrickList);
			cloneScriptList.add(addElement);
		}
		cloneSprite.scriptList = cloneScriptList;

		// update the IDs to preserve the uniqueness of these ids (for example in the stage).
		for (UserBrick cloneBrick : cloneUserBrickList) {
			int newId = cloneBrick.getUserBrickId() + cloneUserBrickList.size();

			List<UserVariable> originalUserBrickVariables = userVariables.getOrCreateVariableListForUserBrick(cloneBrick.getUserBrickId());
			for (UserVariable userVariable : originalUserBrickVariables) {
				userVariables.addUserBrickUserVariableToUserBrick(newId, userVariable.getName(), userVariable.getValue());
			}

			UserScriptDefinitionBrick userScriptDefinitionBrick = cloneBrick.getDefinitionBrick();
			cloneBrick.setUserBrickId(newId);
			cloneBrick.setDefinitionBrick(userScriptDefinitionBrick);
			userScriptDefinitionBrick.setUserBrick(cloneBrick);
		}
		cloneSprite.userBricks = cloneUserBrickList;
		cloneSprite.newUserBrickNext = this.newUserBrickNext;

		cloneSprite.look = this.look.copyLookForSprite(cloneSprite);
		try {
			cloneSprite.look.setLookData(cloneSprite.getLookDataList().get(0));
		} catch (IndexOutOfBoundsException indexOutOfBoundsException) {
			Log.e(TAG, Log.getStackTraceString(indexOutOfBoundsException));
		}

		return cloneSprite;
	}

	public void createWhengamepadButtonScriptActionSequence(String action) {
		ParallelAction whenParallelAction = ExtendedActions.parallel();
		for (Script s : scriptList) {
			if (s instanceof WhenGamepadButtonScript && (((WhenGamepadButtonScript) s).getAction().equalsIgnoreCase(action))) {
				SequenceAction sequence = createActionSequence(s);
				whenParallelAction.addAction(sequence);
			}
		}
		look.setWhenParallelAction(whenParallelAction);
		look.addAction(whenParallelAction);
	}

	public Sprite cloneForBackPack() {
		//TODO: userbricks currently not supported
		final Sprite cloneSprite = new Sprite();
		cloneSprite.setName(this.getName());
		return cloneSprite;
	}

	protected UserBrick findBrickWithId(List<UserBrick> list, int id) {
		for (UserBrick brick : list) {
			if (brick.getUserBrickId() == id) {
				return brick;
			}
		}
		return null;
	}

	public void createWhenScriptActionSequence(String action) {
		ParallelAction whenParallelAction = ExtendedActions.parallel();
		for (Script s : scriptList) {
			if (s instanceof WhenScript && (((WhenScript) s).getAction().equalsIgnoreCase(action))) {
				SequenceAction sequence = createActionSequence(s);
				whenParallelAction.addAction(sequence);
			}
		}
		look.setWhenParallelAction(whenParallelAction);
		look.addAction(whenParallelAction);
	}

	private SequenceAction createActionSequence(Script s) {
		SequenceAction sequence = ExtendedActions.sequence();
		s.run(this, sequence);
		return sequence;
	}

	public void createWhenNfcScriptAction(String uid) {
		ParallelAction whenParallelAction = ExtendedActions.parallel();
		for (Script s : scriptList) {
			if (s instanceof WhenNfcScript) {
				WhenNfcScript whenNfcScript = (WhenNfcScript) s;
				if (whenNfcScript.isMatchAll()
						|| whenNfcScript.getNfcTag().getNfcTagUid().equals(uid)) {
					SequenceAction sequence = createActionSequence(s);
					whenParallelAction.addAction(sequence);
				}
			}
		}
		//TODO: quick fix for faulty behaviour - nfc action triggers again after touchevents
		//look.setWhenParallelAction(whenParallelAction);
		look.addAction(whenParallelAction);
	}

	public void pause() {
		for (Script s : scriptList) {
			s.setPaused(true);
		}
		this.isPaused = true;
	}

	public void resume() {
		for (Script s : scriptList) {
			s.setPaused(false);
		}
		this.isPaused = false;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addScript(Script script) {
		if (script != null && !scriptList.contains(script)) {
			scriptList.add(script);
		}
	}

	public void addScript(int index, Script script) {
		if (script != null && !scriptList.contains(script)) {
			scriptList.add(index, script);
		}
	}

	public Script getScript(int index) {
		if (index < 0 || index >= scriptList.size()) {
			Log.e(TAG, "getScript() Index out of Scope! scriptList size: " + scriptList.size());
			return null;
		}
		return scriptList.get(index);
	}

	public int getNumberOfScripts() {
		if (scriptList != null) {
			return scriptList.size();
		}
		return 0;
	}

	public int getNumberOfBricks() {
		int brickCount = 0;
		for (Script s : scriptList) {
			brickCount += s.getBrickList().size();
		}
		return brickCount;
	}

	public int getScriptIndex(Script script) {
		return scriptList.indexOf(script);
	}

	public void removeAllScripts() {
		scriptList.clear();
	}

	public boolean removeScript(Script script) {
		return scriptList.remove(script);
	}

	public List<LookData> getLookDataList() {
		return lookList;
	}

	public boolean existLookDataByName(LookData look) {
		for (LookData lookdata : lookList) {
			if (lookdata.getLookName().equals(look.getLookName())) {
				return true;
			}
		}
		return false;
	}

	public boolean existLookDataByFileName(LookData look) {
		for (LookData lookdata : lookList) {
			if (lookdata.getLookFileName().equals(look.getLookFileName())) {
				return true;
			}
		}
		return false;
	}

	public void addLookData(LookData data) {
		lookList.add(data);
	}

	public void setLookDataList(List<LookData> list) {
		lookList = list;
	}

	public List<SoundInfo> getSoundList() {
		return soundList;
	}

	public void setSoundList(List<SoundInfo> list) {
		soundList = list;
	}

	public int getRequiredResources() {
		int resources = Brick.NO_RESOURCES;

		for (Script script : scriptList) {
			resources |= script.getRequiredResources();
		}

		for (LookData lookData : getLookDataList()) {
			resources |= lookData.getRequiredResources();
		}

		return resources;
	}

	public List<NfcTagData> getNfcTagList() {
		return nfcTagList;
	}

	public void setNfcTagList(List<NfcTagData> list) {
		nfcTagList = list;
	}

	public int getNextNewUserBrickId() {
		return newUserBrickNext++;
	}

	@Override
	public String toString() {
		return name;
	}

	public boolean containsLookData(LookData lookData) {
		for (LookData lookOfSprite : lookList) {
			if (lookOfSprite.equals(lookData)) {
				return true;
			}
		}
		return false;
	}

	public boolean existSoundInfoByName(SoundInfo sound) {
		for (SoundInfo soundInfo : soundList) {
			if (soundInfo.getTitle().equals(sound.getTitle())) {
				return true;
			}
		}
		return false;
	}

	public boolean existSoundInfoByFileName(SoundInfo sound) {
		for (SoundInfo soundInfo : soundList) {
			if (soundInfo.getSoundFileName().equals(sound.getSoundFileName())) {
				return true;
			}
		}
		return false;
	}

	public void addSound(SoundInfo sound) {
		soundList.add(sound);
	}
}
