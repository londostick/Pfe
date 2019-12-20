#*******************************************************************************
# <copyright>
# Copyright (c) 1995, 2015 AGG developers. All rights reserved. 
# This program and the accompanying materials are made available 
# under the terms of the Eclipse Public License v1.0 which 
# accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
# </copyright>
#*******************************************************************************
#!/bin/sh

for i in $*; do
	echo '// $Id: package.sh,v 1.2 2003/03/05 18:24:15 komm Exp $' > tmp
	echo "package agg.attribute.parser.javaExpr;" >> tmp
	cat $i >> tmp
	mv tmp $i
done
