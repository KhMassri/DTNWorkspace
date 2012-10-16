 s=sum(C(:,2));
c=C(:,2);
p = zeros(size(c));
for i=1:length(p)
p(i)=(s-sum(c(1:i)))/s;
end
s=sum(CC(:,2));
c=CC(:,2);
pp = zeros(size(c));
for i=1:length(pp)
pp(i)=(s-sum(c(1:i)))/s;
end
s=sum(CCC(:,2));
c=CCC(:,2);
ppp = zeros(size(c));
for i=1:length(ppp)
ppp(i)=(s-sum(c(1:i)))/s;
end

