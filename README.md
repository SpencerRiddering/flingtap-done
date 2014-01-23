FlingTap Done
==========================

A feature rich to-do list for Android.


Background
-----

* FlingTap Done placed in the [ADC 2](https://developers.google.com/android/adc/gallery_productivitytools) and then was published on [Android Market (now Google Play)](https://play.google.com/store/apps/details?id=com.flingtap.done.base&hl=en). 
* Features such as purchasing unlockable app content (prior to Android Market in-app purchases), caller and location reminders were at the bleeding edge when the app was released.
* Video demos: 
 * [Create, Edit, Update, Complete and Delete Tasks](http://bit.ly/4miFRZ) (YouTube)
  Explains the most common steps including creating a new task, editing a task, updating a task, attaching content to a task, marking the task complete and finally deleting the task.
 * [Filtering and Searching the Task List](http://bit.ly/24Ntp3) (YouTube)
  Explains the various ways to you can filter the task list and search for tasks. Filter search, search, filters, filter controls and filter switching are all explained.
 * [Labels](http://bit.ly/4ffJZ7) (YouTube)
  Explains what labels are and how labels are used to organize tasks. Demonstrates how to filter using labels.
 * [Archive](http://bit.ly/1GM1D4) (YouTube)
  Explains what the archive is and demonstrates how to use it to save completed tasks. Demonstrates how filters are used to view archived tasks.
 * [Callminders and Nearminders](http://bit.ly/10q6WU) (YouTube)
  Explains what Callminders and Nearminders are. Demonstrates how Callminders are enabled and how to act on them. Demonstrates how to add Nearminders and act on them.


Interesting parts
-----

* Label database implementation
* Filtering using labels (FilterUtil)
 * Labels used for distingushing between repositories (archive/main)
 * User applied labels vs Permanent labels.
* Attachments implementation
* Error Handling (ErrorUtil.java)
* Content provider URL params
* Persisting Notification data 
* Persisting App Widget settings and deleted widgets (yes, this was/is required)
* Working with backup service
* License checking (LicenseUtil)
* Activity Participants (ContextActivityParticipant). Long before Android Fragments were introduced, I attempted to solve a similar problem. 
* Delegated presentation for List Items. (FilterElementDelegatingListAdapterDelegate and UriDelegateMapping)
* Combined Participant and WizardStep for SelectAreaActivity with good result.
* Managing dialogs with a SparseArray (mManagedDialogs = new SparseArray<Dialog>(); in     TaskEditor.java)
* Working example of implementing [searchable interface](http://developer.android.com/guide/topics/search/search-dialog.html) (TaskList.java, searchable.xml, TaskProvider.java) 
* Backup service implementation
 * Serialize/Deserialize database (TaskProvider.serializeDb() )
* Wizard (Wizard,AbstractWizardStep)
* Use of asserts in Android.
* Implementation of widget.

Broken
-----
* Build doesn't pull GOOGLE_BACKUP_API_KEY and ADMOB_PUB_ID into AndroidManifest.xml files correctly. This still needs to be done manually (search for command.line.admobPubId and command.line.googleBackupApiKey in AndroidManifest files).

Usage
-----
* Maven build. 
* Build scripts (build-debug.sh, build-test.sh, and build-release.sh) demonstrate how to launch the build process.
* See [SETUP](./SETUP.md) for details. 
 
License
-------

    Copyright 2009 Lean & Keen, LLC 

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
