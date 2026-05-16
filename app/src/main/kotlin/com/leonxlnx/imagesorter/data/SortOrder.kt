package com.leonxlnx.imagesorter.data

/** How the swipe queue should be ordered before it is presented to the user. */
enum class SortOrder(val id: String, val label: String) {
    NewestFirst("newest", "Newest first"),
    OldestFirst("oldest", "Oldest first"),
    LargestFirst("largest", "Largest first"),
    SmallestFirst("smallest", "Smallest first"),
    Random("random", "Random");

    companion object {
        fun fromId(id: String?): SortOrder = entries.firstOrNull { it.id == id } ?: NewestFirst
    }
}

/** Where created folders live under public storage. */
enum class FolderRoot(val id: String, val label: String, val relativePathPrefix: String) {
    Pictures("pictures", "Pictures / PhotoSwipe", "Pictures/PhotoSwipe"),
    DCIM("dcim", "DCIM / PhotoSwipe", "DCIM/PhotoSwipe");

    companion object {
        fun fromId(id: String?): FolderRoot = entries.firstOrNull { it.id == id } ?: Pictures
    }
}
