/*
 * Created on 9 Sep 2022
 *
 * author dimitry
 */
package org.freeplane.api;

public enum ChildNodesAlignment {
	NOT_SET(false),
	AFTER_PARENT(Placement.TOP, true),
	FIRST_CHILD_BY_PARENT(Placement.TOP, false),
	BY_CENTER(Placement.CENTER, false), FLOW(Placement.CENTER, false),
	LAST_CHILD_BY_PARENT(Placement.BOTTOM, false),
	BEFORE_PARENT(Placement.BOTTOM, true),
	AUTO(Placement.CENTER, false),
	STACKED_AUTO(true);

	public enum Placement{
		TOP {
			@Override
			public int align(int value) {
				return 0;
			}
		},
		CENTER {
			@Override
			public int align(int value) {
				return value / 2;
			}
		},
		BOTTOM {
			@Override
			public int align(int value) {
				return value;
			}
		},
		UNKNOWN {
			@Override
			public int align(int value) {
				throw new IllegalStateException("Can't align for unknown placement");
			}
		};
		public abstract int align(int value);
	}

    private final boolean isStacked;
    private final Placement placement;

    private ChildNodesAlignment(boolean isStacked) {
    	this(Placement.UNKNOWN, isStacked);
    }
    private ChildNodesAlignment(Placement placement, boolean isStacked) {
        this.placement = placement;
		this.isStacked = isStacked;
    }

    public boolean isStacked() {
        return isStacked;
    }

    public int align(int value) {
    	return placement.align(value);
    }

    public Placement placement() {
		return placement;
	}


}
