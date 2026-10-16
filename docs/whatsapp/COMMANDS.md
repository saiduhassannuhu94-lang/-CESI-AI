# CESI WhatsApp command guide

This guide documents commands currently covered by parser tests. WhatsApp UI execution still depends on the installed WhatsApp version and the Android permissions/consent described below.

## Reactions

Examples:
- `ka daura reaction na dariya`
- `ka daura wa wannan sakon reaction na dariya`
- `react with funny emoji`
- `react to the latest message with ❤️`

Reaction commands use the latest in-memory WhatsApp notification context. If CESI has no usable recent message context, it must ask for a new message/context rather than guessing. Accessibility automation must be enabled with the current disclosure. A UI click is not proof that WhatsApp synchronized the reaction.

## Reply to an older message

Examples:
- `reply to the old message "I'll call you later" with "Sure, talk soon"`
- `amsa saƙon "Ina zuwa gobe" da "To, zan jira ka"`

CESI asks for confirmation before sending. After confirming, open the correct chat and use WhatsApp's own search if needed to make the exact quoted message visible. The request expires after three minutes. If the target message, Reply menu item, composer, or Send button is not unique, CESI stops instead of guessing. The quoted text is kept in memory only for this request.

## Share media with a caption

Examples:
- `upload a video with caption: New project trailer`
- `post this photo as WhatsApp status with caption: Eid Mubarak to everyone`

CESI opens Android's system file picker. Select the image/video; the picker grants access to only that selected item, so no broad gallery permission is needed. WhatsApp then opens with the media and caption. Review the destination and caption, then press Send or Post yourself. Whether My Status is available in WhatsApp's share target list depends on WhatsApp version.

If you omit a caption, CESI uses a generic caption based on the saved local tone preference. It does not claim to have visually analyzed the selected media.

## Teach CESI your preferred caption tone

Examples:
- `remember my caption style as funny`
- `for future captions use professional`
- `ka rika amfani da salon barkwanci wajen captions`
- `clear caption style`

Supported tones: natural, funny, professional, Islamic, romantic, and short. CESI stores only the chosen tone in local app preferences. It does not store previous captions, message bodies, or media, and this is not model retraining.

## Important testing note

Passing parser/unit tests proves that a command is recognized by code. It does not prove that the Accessibility service can find the correct controls on a particular phone/WhatsApp version. Real-device validation is still required before claiming the end-to-end action works.
