package org.freeplane.features.bookmarks.mindmapmode;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

class BookmarkTransferables {
	static final DataFlavor BOOKMARK_FLAVOR = new DataFlavor(NodeBookmark.class, "NodeBookmark");

	static class BookmarkTransferable implements Transferable {
		private final int sourceIndex;

		public BookmarkTransferable(int sourceIndex) {
			this.sourceIndex = sourceIndex;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[]{BOOKMARK_FLAVOR};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return BOOKMARK_FLAVOR.equals(flavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (!isDataFlavorSupported(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
			return sourceIndex;
		}
	}

	static class CombinedTransferable implements Transferable {
		private final Transferable bookmarkTransferable;
		private final Transferable nodeTransferable;

		public CombinedTransferable(Transferable bookmarkTransferable, Transferable nodeTransferable) {
			this.bookmarkTransferable = bookmarkTransferable;
			this.nodeTransferable = nodeTransferable;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] bookmarkFlavors = bookmarkTransferable.getTransferDataFlavors();
			DataFlavor[] nodeFlavors = nodeTransferable.getTransferDataFlavors();
			DataFlavor[] combined = new DataFlavor[bookmarkFlavors.length + nodeFlavors.length];
			System.arraycopy(bookmarkFlavors, 0, combined, 0, bookmarkFlavors.length);
			System.arraycopy(nodeFlavors, 0, combined, bookmarkFlavors.length, nodeFlavors.length);
			return combined;
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return bookmarkTransferable.isDataFlavorSupported(flavor) || nodeTransferable.isDataFlavorSupported(flavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (bookmarkTransferable.isDataFlavorSupported(flavor)) {
				return bookmarkTransferable.getTransferData(flavor);
			} else if (nodeTransferable.isDataFlavorSupported(flavor)) {
				return nodeTransferable.getTransferData(flavor);
			}
			throw new UnsupportedFlavorException(flavor);
		}
	}
} 