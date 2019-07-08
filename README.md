# ChatScreen-Presentation
### Logic for listening and sending messages

1. Get users data, save to local db and return other user data for showing in UI                    
2. Get once all messages                                                                            
3. Subscribe to Firebase Users current room data (for current and other users changes)              
4. Receive changes from current and show new messages                                               
5. Send unread message count 0
6. Receive changes from other user and change seen messages indicator                               
7. Send new Message to Firebase db and update two users data in Firebase database Users model room  


### Firebase database structure

Messages:

	-messages
		-roomid
			-timestamp
			-senderId
			-senderName
			-senderPhoto
			-text
			

User data:

	-user
		-userId
			-data
				-online
				-photoUrl
				-profileName
			-room
				-roomId
				-message
				-otherUserId
				-timestamp
				-blockedOtherUser
				-unreadCount
